package ru.practicum.mapper;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.user.UserAction;
import ru.practicum.model.user.enums.ActionType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserActionMapper {
    public static UserAction mapToEntity(UserActionAvro userActionAvro) {
        return new UserAction(
                null,
                userActionAvro.getUserId(),
                userActionAvro.getEventId(),
                ActionType.valueOf(userActionAvro.getActionType().name()),
                userActionAvro.getTimestamp());
    }
}
