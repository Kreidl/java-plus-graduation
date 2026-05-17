package ru.practicum.conroller;

import ru.practicum.ewm.stats.proto.*;
import ru.practicum.service.RecommendationsService;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationsService recommendationsService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto requestProto,
            StreamObserver<RecommendedEventProto> responseObserver) {

        log.info(
                "gRPC request: getRecommendationsForUser(userId={}, maxResults={})",
                requestProto.getUserId(),
                requestProto.getMaxResults());

        try {
            recommendationsService
                    .getRecommendationsForUser(requestProto)
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();
            log.debug("gRPC response completed: getRecommendationsForUser");
        } catch (Exception e) {
            log.error(
                    "Error processing getRecommendationsForUser request: userId={}, error={}",
                    requestProto.getUserId(),
                    e.getMessage(),
                    e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto requestProto,
            StreamObserver<RecommendedEventProto> responseObserver) {

        log.info(
                "gRPC request: getSimilarEvents(eventId={}, userId={}, maxResults={})",
                requestProto.getEventId(),
                requestProto.getUserId(),
                requestProto.getMaxResults());

        try {
            recommendationsService.getSimilarEvents(requestProto).forEach(responseObserver::onNext);

            responseObserver.onCompleted();
            log.debug("gRPC response completed: getSimilarEvents");
        } catch (Exception e) {
            log.error(
                    "Error processing getSimilarEvents request: eventId={}, error={}",
                    requestProto.getEventId(),
                    e.getMessage(),
                    e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto requestProto,
            StreamObserver<RecommendedEventProto> responseObserver) {

        log.info("gRPC request: getInteractionsCount(eventIds={})", requestProto.getEventIdList());

        try {
            recommendationsService
                    .getUserActionsCount(requestProto)
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();
            log.debug("gRPC response completed: getInteractionsCount");
        } catch (Exception e) {
            log.error(
                    "Error processing getInteractionsCount request: eventIds={}, error={}",
                    requestProto.getEventIdList(),
                    e.getMessage(),
                    e);
            responseObserver.onError(e);
        }
    }
}
