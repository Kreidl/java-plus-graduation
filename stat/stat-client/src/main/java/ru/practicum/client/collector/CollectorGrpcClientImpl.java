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
        client.collectUserAction(userActionProto);
    }
}
