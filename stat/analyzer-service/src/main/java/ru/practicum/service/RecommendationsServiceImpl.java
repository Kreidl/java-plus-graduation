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
        List<UserAction> touchedEvents =
                userActionRepository.findEventsByUserIdOrderByTimestampDesc(
                        requestProto.getUserId(), requestProto.getMaxResults());

        if (touchedEvents.isEmpty()) {
            return Stream.empty();
        }
        // Уже взаимодействовал
        List<Long> touchedEventIds = touchedEvents.stream().map(UserAction::getEventId).toList();

        // scopes тех, с которыми взаимодействовал
        Map<Long, Double> eventScopes =
                touchedEvents.stream()
                        .collect(
                                Collectors.toMap(
                                        UserAction::getEventId,
                                        userAction ->
                                                convertActionTypeToWeight(
                                                        userAction.getActionType()),
                                        Math::max));

        // Похожие
        List<EventSimilarity> similarEvents =
                eventSimilarityRepository.findSimilarEvents(
                        touchedEventIds, requestProto.getMaxResults());

        // ids похожих, исключая те, с которыми взаимодействовал
        List<Long> similarEventsIds =
                similarEvents.stream().map(EventSimilarity::getEventB).toList();

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
            }
        }
        return candidateScores.entrySet().stream()
                .sorted((event1, event2) -> Double.compare(event2.getValue(), event1.getValue()))
                .map(
                        entry ->
                                RecommendedEventProto.newBuilder()
                                        .setEventId(entry.getKey())
                                        .setScore(entry.getValue())
                                        .build());
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto requestProto) {
        List<Long> eventsIds = userActionRepository.findEventsIdByUserId(requestProto.getUserId());
        if (eventsIds.isEmpty()) {
            return Stream.empty();
        }
        eventsIds.remove(requestProto.getEventId());
        List<EventSimilarity> similarEvents =
                eventSimilarityRepository.findSimilarEventsByEventsIds(
                        requestProto.getEventId(), eventsIds);

        Map<Long, Double> similarIdsWithScope = new HashMap<>();
        for (EventSimilarity eventSimilarity : similarEvents) {
            Long similarEventId =
                    requestProto.getEventId() == eventSimilarity.getEventA()
                            ? eventSimilarity.getEventB()
                            : eventSimilarity.getEventA();
            similarIdsWithScope.put(similarEventId, eventSimilarity.getScore());
        }
        return similarIdsWithScope.entrySet().stream()
                .sorted((event1, event2) -> Double.compare(event2.getValue(), event1.getValue()))
                .map(
                        entry ->
                                RecommendedEventProto.newBuilder()
                                        .setEventId(entry.getKey())
                                        .setScore(entry.getValue())
                                        .build());
    }

    @Override
    public Stream<RecommendedEventProto> getUserActionsCount(
            InteractionsCountRequestProto requestProto) {
        List<UserAction> userActions =
                userActionRepository.findUserActionByEventIdIn(requestProto.getEventIdList());
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
                            return RecommendedEventProto.newBuilder()
                                    .setEventId(eventId)
                                    .setScore(weight)
                                    .build();
                        });
    }

    private Double convertActionTypeToWeight(ActionType actionType) {
        switch (actionType) {
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
                return 0.0;
            }
        }
    }
}
