package ru.practicum.controller;

import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.service.handler.UserActionHandler;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionHandler userActionHandler;

    @Override
    public void collectUserAction(
            UserActionProto userActionProto, StreamObserver<Empty> responseObserver) {

        log.info(
                "gRPC request: collectUserAction(userId={}, eventId={}, actionType={})",
                userActionProto.getUserId(),
                userActionProto.getEventId(),
                userActionProto.getActionType());

        try {
            log.debug("Processing user action: {}", userActionProto);
            userActionHandler.handle(userActionProto);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.debug("gRPC response completed successfully: collectUserAction");

        } catch (Exception e) {
            log.error(
                    "Error processing user action: userId={}, eventId={}, actionType={}, error={}",
                    userActionProto.getUserId(),
                    userActionProto.getEventId(),
                    userActionProto.getActionType(),
                    e.getMessage(),
                    e);

            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.INTERNAL
                                    .withDescription(
                                            "Failed to process user action: " + e.getMessage())
                                    .withCause(e)));
        }
    }
}
