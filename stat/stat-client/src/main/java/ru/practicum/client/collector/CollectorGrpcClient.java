package ru.practicum.client.collector;

import ru.practicum.ewm.stats.proto.UserActionProto;

public interface CollectorGrpcClient {
    void sendUserAction(UserActionProto userActionProto);
}
