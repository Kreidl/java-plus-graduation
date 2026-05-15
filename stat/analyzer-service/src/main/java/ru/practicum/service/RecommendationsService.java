package ru.practicum.service;

import java.util.stream.Stream;

import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

public interface RecommendationsService {
    Stream<RecommendedEventProto> getRecommendationsForUser(
            UserPredictionsRequestProto requestProto);

    Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto requestProto);

    Stream<RecommendedEventProto> getUserActionsCount(InteractionsCountRequestProto requestProto);
}
