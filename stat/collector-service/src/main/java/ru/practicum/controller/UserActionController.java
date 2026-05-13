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
        try {
            log.info("Saving user action {}", userActionProto.toString());
            userActionHandler.handle(userActionProto);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    new StatusRuntimeException(
                            Status.INTERNAL.withDescription(e.getLocalizedMessage()).withCause(e)));
        }
    }
}
