package ru.practicum.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.practicum.ewm.stats.proto.*;
import ru.practicum.model.event.EventSimilarity;
import ru.practicum.model.user.UserAction;
import ru.practicum.model.user.enums.ActionType;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationsServiceImpl implements RecommendationsService {
    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;
    private final Double VIEWING_RATE = 0.4;
    private final Double REGISTRATION_RATE = 0.8;
    private final Double LIKE_RATE = 1.0;

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(
            UserPredictionsRequestProto requestProto) {

        log.info(
                "Generating recommendations for userId={}, maxResults={}",
                requestProto.getUserId(),
                requestProto.getMaxResults());

        List<UserAction> touchedEvents =
                userActionRepository.findEventsByUserIdOrderByTimestampDesc(
                        requestProto.getUserId(), requestProto.getMaxResults());

        if (touchedEvents.isEmpty()) {
            log.debug(
                    "No user actions found for userId={}, returning empty recommendations",
                    requestProto.getUserId());
            return Stream.empty();
        }

        log.debug(
                "Found {} recent user actions for userId={}",
                touchedEvents.size(),
                requestProto.getUserId());

        // Extract event IDs the user has interacted with
        List<Long> touchedEventIds = touchedEvents.stream().map(UserAction::getEventId).toList();

        // Calculate weights (scopes) for touched events
        Map<Long, Double> eventScopes =
                touchedEvents.stream()
                        .collect(
                                Collectors.toMap(
                                        UserAction::getEventId,
                                        userAction ->
                                                convertActionTypeToWeight(
                                                        userAction.getActionType()),
                                        Math::max));

        log.debug("Calculated event scopes for {} events", eventScopes.size());

        // Find similar events based on touched events
        List<EventSimilarity> similarEvents =
                eventSimilarityRepository.findSimilarEvents(
                        touchedEventIds, requestProto.getMaxResults());

        log.debug("Found {} similar event pairs from repository", similarEvents.size());

        // Calculate candidate scores for recommendations
        Map<Long, Double> candidateScores = new HashMap<>();
        for (EventSimilarity eventSimilarity : similarEvents) {
            Long candidateEventId =
                    touchedEventIds.contains(eventSimilarity.getEventA())
                            ? eventSimilarity.getEventB()
                            : eventSimilarity.getEventA();

            Long userEventId =
                    touchedEventIds.contains(eventSimilarity.getEventA())
                            ? eventSimilarity.getEventA()
                            : eventSimilarity.getEventB();

            Double rate = eventScopes.get(userEventId);
            if (rate != null) {
                double score = rate * eventSimilarity.getScore();
                candidateScores.merge(candidateEventId, score, Math::max);
                log.trace(
                        "Calculated score for candidate event {}: {} * {} = {}",
                        candidateEventId,
                        rate,
                        eventSimilarity.getScore(),
                        score);
            }
        }

        // Sort by score descending and map to proto
        Stream<RecommendedEventProto> result =
                candidateScores.entrySet().stream()
                        .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                        .map(
                                entry ->
                                        RecommendedEventProto.newBuilder()
                                                .setEventId(entry.getKey())
                                                .setScore(entry.getValue())
                                                .build());

        log.info(
                "Generated {} recommendations for userId={}",
                candidateScores.size(),
                requestProto.getUserId());

        return result;
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto requestProto) {
        log.info(
                "Finding similar events for eventId={}, userId={}, maxResults={}",
                requestProto.getEventId(),
                requestProto.getUserId(),
                requestProto.getMaxResults());

        List<Long> eventsIds = userActionRepository.findEventsIdByUserId(requestProto.getUserId());
        if (eventsIds.isEmpty()) {
            log.debug(
                    "No events found for userId={}, returning empty similar events",
                    requestProto.getUserId());
            return Stream.empty();
        }

        // Exclude the target event from the list
        eventsIds.remove(requestProto.getEventId());
        log.debug(
                "Found {} user events (excluding target) for similarity search", eventsIds.size());

        List<EventSimilarity> similarEvents =
                eventSimilarityRepository.findSimilarEventsByEventsIds(
                        requestProto.getEventId(), eventsIds);

        log.debug(
                "Found {} direct similarity records for eventId={}",
                similarEvents.size(),
                requestProto.getEventId());

        // Build map of similar event IDs with their scores
        Map<Long, Double> similarIdsWithScope = new HashMap<>();
        for (EventSimilarity eventSimilarity : similarEvents) {
            Long similarEventId =
                    requestProto.getEventId() == eventSimilarity.getEventA()
                            ? eventSimilarity.getEventB()
                            : eventSimilarity.getEventA();
            similarIdsWithScope.put(similarEventId, eventSimilarity.getScore());
            log.trace(
                    "Added similar event: id={}, score={}",
                    similarEventId,
                    eventSimilarity.getScore());
        }

        // Sort by score descending and map to proto
        Stream<RecommendedEventProto> result =
                similarIdsWithScope.entrySet().stream()
                        .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                        .map(
                                entry ->
                                        RecommendedEventProto.newBuilder()
                                                .setEventId(entry.getKey())
                                                .setScore(entry.getValue())
                                                .build());

        log.info(
                "Returning {} similar events for eventId={}",
                similarIdsWithScope.size(),
                requestProto.getEventId());

        return result;
    }

    @Override
    public Stream<RecommendedEventProto> getUserActionsCount(
            InteractionsCountRequestProto requestProto) {

        log.debug("Calculating interaction counts for eventIds={}", requestProto.getEventIdList());

        List<UserAction> userActions =
                userActionRepository.findUserActionByEventIdIn(requestProto.getEventIdList());
        log.debug("Found {} user actions matching requested event IDs", userActions.size());

        return requestProto.getEventIdList().stream()
                .map(
                        eventId -> {
                            double weight =
                                    userActions.stream()
                                            .filter(
                                                    userAction ->
                                                            userAction.getEventId().equals(eventId))
                                            .mapToDouble(
                                                    userAction ->
                                                            convertActionTypeToWeight(
                                                                    userAction.getActionType()))
                                            .sum();

                            log.trace(
                                    "Calculated total weight for eventId={}: {}", eventId, weight);

                            return RecommendedEventProto.newBuilder()
                                    .setEventId(eventId)
                                    .setScore(weight)
                                    .build();
                        });
    }

    private Double convertActionTypeToWeight(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> VIEWING_RATE;
            case REGISTER -> REGISTRATION_RATE;
            case LIKE -> LIKE_RATE;
            default -> {
                log.warn("Unknown action type for weight conversion: {}", actionType);
                yield 0.0;
            }
        };
    }
}
