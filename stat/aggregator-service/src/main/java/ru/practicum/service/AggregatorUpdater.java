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
        log.info("Start to process message");
        UserActionAvro userActionAvro = record.value();
        Long eventId = userActionAvro.getEventId();
        Long userId = userActionAvro.getUserId();
        Double newWeight = convertActionTypeToWeight(userActionAvro.getActionType());

        Map<Long, Double> eventWeights = eventsUserMaxWeight.get(eventId);
        Double oldWeight = (eventWeights != null) ? eventWeights.getOrDefault(userId, 0.0) : 0.0;
        if (newWeight <= oldWeight) {
            log.debug("Weight has not increased");
            return List.of();
        }

        eventWeights = eventsUserMaxWeight.computeIfAbsent(eventId, k -> new HashMap<>());
        eventWeights.put(userId, newWeight);
        double diff = newWeight - oldWeight;
        double newSum = eventsSumWeights.getOrDefault(eventId, 0.0) + diff;
        eventsSumWeights.put(eventId, newSum);
        log.debug(
                "Event with id={} updated, user with id={}, oldWeight={}, newWeight={}, newSum={}",
                eventId,
                userId,
                oldWeight,
                newWeight,
                newSum);
        return calculateEventSimilarity(
                eventId, userId, oldWeight, newWeight, userActionAvro.getTimestamp());
    }

    private List<EventSimilarityAvro> calculateEventSimilarity(
            long eventId, long userId, double oldWeight, double newWeight, Instant timestamp) {
        List<EventSimilarityAvro> eventSimilarityList = new ArrayList<>();
        for (long otherEventId : eventsUserMaxWeight.keySet()) {
            if (otherEventId == eventId) continue;
            Map<Long, Double> eventWeights = eventsUserMaxWeight.get(otherEventId);
            Double otherWeight = (eventWeights != null) ? eventWeights.get(userId) : null;
            if (otherWeight == null) continue;

            double similarity = 0.0;
            // min eventId
            long first = Math.min(eventId, otherEventId);
            // max eventId
            long second = Math.max(eventId, otherEventId);
            Map<Long, Double> pairSums =
                    eventsMinWeightsSum.computeIfAbsent(first, k -> new HashMap<>());
            double currentMin = pairSums.getOrDefault(second, 0.0);
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double diff = newMin - oldMin;

            pairSums.put(second, currentMin + diff);

            // Similarity
            Double sum1 = eventsSumWeights.get(first);
            Double sum2 = eventsSumWeights.get(second);
            if (sum1 == null || sum2 == null || sum1 <= 0 || sum2 <= 0) {
                similarity = 0.0;
            } else {
                double sMin = pairSums.get(second);
                similarity = sMin / Math.sqrt(sum1 * sum2);
            }
            if (similarity > 0) {
                eventSimilarityList.add(
                        EventSimilarityAvro.newBuilder()
                                .setEventA(first)
                                .setEventB(second)
                                .setScore(similarity)
                                .setTimestamp(timestamp)
                                .build());
            }
        }
        return eventSimilarityList;
    }

    private Double convertActionTypeToWeight(ActionTypeAvro actionTypeAvro) {
        switch (actionTypeAvro) {
            case VIEW -> {
                return VIEWING_RATE;
            }
            case REGISTER -> {
                return REGISTRATION_RATE;
            }
            case LIKE -> {
                return LIKE_RATE;
            }
            default -> {
                log.warn("Invalid user action type: {}", actionTypeAvro);
                throw new ActionTypeNotFound("Invalid user action type: " + actionTypeAvro);
            }
        }
    }
}
