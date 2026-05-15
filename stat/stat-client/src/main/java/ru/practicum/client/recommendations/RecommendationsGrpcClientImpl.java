package ru.practicum.client.recommendations;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.*;

import ru.practicum.ewm.stats.proto.*;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@Component
public class RecommendationsGrpcClientImpl implements RecommendationsGrpcClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(Long userId, long maxResults) {
        UserPredictionsRequestProto request =
                UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build();
        Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);
        return asStream(iterator);
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(
            Long eventId, Long userId, long maxResults) {
        SimilarEventsRequestProto request =
                SimilarEventsRequestProto.newBuilder()
                        .setEventId(eventId)
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build();
        Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
        return asStream(iterator);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(Set<Long> eventIds) {
        InteractionsCountRequestProto request =
                InteractionsCountRequestProto.newBuilder().addAllEventId(eventIds).build();
        Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
        return asStream(iterator);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(Long eventId) {
        return getInteractionsCount(Collections.singleton(eventId));
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
