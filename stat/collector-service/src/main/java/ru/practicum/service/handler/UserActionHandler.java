package ru.practicum.service.handler;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

public interface UserActionHandler {
    UserActionAvro protoToAvro(UserActionProto userActionProto);

    void handle(UserActionProto userActionProto);
}
