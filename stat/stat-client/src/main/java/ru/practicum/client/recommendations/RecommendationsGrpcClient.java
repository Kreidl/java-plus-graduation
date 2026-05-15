package ru.practicum.client.recommendations;

import java.util.Set;
import java.util.stream.Stream;

import ru.practicum.ewm.stats.proto.RecommendedEventProto;

public interface RecommendationsGrpcClient {
    Stream<RecommendedEventProto> getRecommendationsForUser(Long userId, long maxResults);

    Stream<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, long maxResults);

    Stream<RecommendedEventProto> getInteractionsCount(Set<Long> eventIds);

    Stream<RecommendedEventProto> getInteractionsCount(Long eventId);
}
