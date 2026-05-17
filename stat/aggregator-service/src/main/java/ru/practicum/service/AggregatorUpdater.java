package ru.practicum.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.exception.ActionTypeNotFound;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
@Component
public class AggregatorUpdater {
    // Events, (User, MaxWeight)
    private final Map<Long, Map<Long, Double>> eventsUserMaxWeight = new HashMap<>();
    // Events, SumWeight
    private final Map<Long, Double> eventsSumWeights = new HashMap<>();
    // Event1, (Event2, SumMinWeight)
    private final Map<Long, Map<Long, Double>> eventsMinWeightsSum = new HashMap<>();
    private final Double VIEWING_RATE = 0.4;
    private final Double REGISTRATION_RATE = 0.8;
    private final Double LIKE_RATE = 1.0;

    public List<EventSimilarityAvro> handle(ConsumerRecord<Long, UserActionAvro> record) {
        log.debug(
                "Processing user action: key={}, offset={}, partition={}",
                record.key(),
                record.offset(),
                record.partition());

        UserActionAvro userActionAvro = record.value();
        Long eventId = userActionAvro.getEventId();
        Long userId = userActionAvro.getUserId();
        Double newWeight = convertActionTypeToWeight(userActionAvro.getActionType());

        log.trace(
                "User action details: eventId={}, userId={}, actionType={}, weight={}",
                eventId,
                userId,
                userActionAvro.getActionType(),
                newWeight);

        Map<Long, Double> eventWeights = eventsUserMaxWeight.get(eventId);
        Double oldWeight = (eventWeights != null) ? eventWeights.getOrDefault(userId, 0.0) : 0.0;

        if (newWeight <= oldWeight) {
            log.debug(
                    "Weight unchanged or decreased for user {} on event {}: old={}, new={}",
                    userId,
                    eventId,
                    oldWeight,
                    newWeight);
            return List.of();
        }

        // Update max weight for this user-event pair
        eventWeights = eventsUserMaxWeight.computeIfAbsent(eventId, k -> new HashMap<>());
        eventWeights.put(userId, newWeight);

        // Update sum of weights for this event
        double diff = newWeight - oldWeight;
        double newSum = eventsSumWeights.getOrDefault(eventId, 0.0) + diff;
        eventsSumWeights.put(eventId, newSum);

        log.debug(
                "Updated weights for event {}: user={}, oldWeight={}, newWeight={}, newSum={}",
                eventId,
                userId,
                oldWeight,
                newWeight,
                newSum);

        // Calculate and return updated similarities
        return calculateEventSimilarity(
                eventId, userId, oldWeight, newWeight, userActionAvro.getTimestamp());
    }

    private List<EventSimilarityAvro> calculateEventSimilarity(
            long eventId, long userId, double oldWeight, double newWeight, Instant timestamp) {

        log.trace("Calculating similarities for event {} after user {} action", eventId, userId);

        List<EventSimilarityAvro> eventSimilarityList = new ArrayList<>();

        for (long otherEventId : eventsUserMaxWeight.keySet()) {
            if (otherEventId == eventId) continue;

            Map<Long, Double> eventWeights = eventsUserMaxWeight.get(otherEventId);
            Double otherWeight = (eventWeights != null) ? eventWeights.get(userId) : null;

            if (otherWeight == null) {
                log.trace(
                        "User {} has no interaction with event {}, skipping similarity calculation",
                        userId,
                        otherEventId);
                continue;
            }

            // Ensure consistent ordering: first < second
            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            Map<Long, Double> pairSums =
                    eventsMinWeightsSum.computeIfAbsent(first, k -> new HashMap<>());
            double currentMin = pairSums.getOrDefault(second, 0.0);
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double diff = newMin - oldMin;

            pairSums.put(second, currentMin + diff);
            log.trace(
                    "Updated min-weight sum for pair ({}, {}): diff={}, newSum={}",
                    first,
                    second,
                    diff,
                    currentMin + diff);

            // Calculate cosine similarity
            Double sum1 = eventsSumWeights.get(first);
            Double sum2 = eventsSumWeights.get(second);

            double similarity = 0.0;
            if (sum1 != null && sum2 != null && sum1 > 0 && sum2 > 0) {
                double sMin = pairSums.get(second);
                similarity = sMin / Math.sqrt(sum1 * sum2);
                log.trace("Calculated similarity for pair ({}, {}): {}", first, second, similarity);
            }

            if (similarity > 0) {
                EventSimilarityAvro similarityAvro =
                        EventSimilarityAvro.newBuilder()
                                .setEventA(first)
                                .setEventB(second)
                                .setScore(similarity)
                                .setTimestamp(timestamp)
                                .build();
                eventSimilarityList.add(similarityAvro);
                log.debug(
                        "Generated similarity record: eventA={}, eventB={}, score={}",
                        first,
                        second,
                        similarity);
            }
        }

        log.debug(
                "Generated {} similarity records for event {}",
                eventSimilarityList.size(),
                eventId);
        return eventSimilarityList;
    }

    private Double convertActionTypeToWeight(ActionTypeAvro actionTypeAvro) {
        return switch (actionTypeAvro) {
            case VIEW -> {
                log.trace("Converted VIEW action to weight {}", VIEWING_RATE);
                yield VIEWING_RATE;
            }
            case REGISTER -> {
                log.trace("Converted REGISTER action to weight {}", REGISTRATION_RATE);
                yield REGISTRATION_RATE;
            }
            case LIKE -> {
                log.trace("Converted LIKE action to weight {}", LIKE_RATE);
                yield LIKE_RATE;
            }
            default -> {
                log.warn("Unknown action type encountered: {}", actionTypeAvro);
                throw new ActionTypeNotFound("Invalid user action type: " + actionTypeAvro);
            }
        };
    }
}
