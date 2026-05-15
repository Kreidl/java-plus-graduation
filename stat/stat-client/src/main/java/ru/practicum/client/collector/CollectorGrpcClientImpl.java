package ru.practicum.client.collector;

import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@Component
public class CollectorGrpcClientImpl implements CollectorGrpcClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    @Override
    public void sendUserAction(UserActionProto userActionProto) {
        log.trace("Sending user action via gRPC: userId={}, eventId={}, actionType={}",
                userActionProto.getUserId(),
                userActionProto.getEventId(),
                userActionProto.getActionType());

        try {
            client.collectUserAction(userActionProto);
            log.debug("User action sent successfully to collector service");
        } catch (Exception e) {
            log.error("Failed to send user action to collector service: userId={}, eventId={}, error={}",
                    userActionProto.getUserId(),
                    userActionProto.getEventId(),
                    e.getMessage(), e);
            throw e;
        }
    }
}
