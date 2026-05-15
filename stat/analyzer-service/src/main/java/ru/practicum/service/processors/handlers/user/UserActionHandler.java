package ru.practicum.service.processors.handlers.user;

import java.util.Optional;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.exception.ActionTypeNotFound;
import ru.practicum.model.user.UserAction;
import ru.practicum.model.user.enums.ActionType;
import ru.practicum.repository.UserActionRepository;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionHandler {
    private final UserActionRepository userActionRepository;
    private final Double VIEWING_RATE = 0.4;
    private final Double REGISTRATION_RATE = 0.8;
    private final Double LIKE_RATE = 1.0;

    public void handle(UserActionAvro userActionAvro) {
        Optional<UserAction> userActionOpt =
                userActionRepository.findByUserIdAndEventId(
                        userActionAvro.getUserId(), userActionAvro.getEventId());
        if (userActionOpt.isEmpty()) {
            createUserAction(userActionAvro);
        } else {
            updateUserAction(userActionOpt.get(), userActionAvro);
        }
    }

    private void createUserAction(UserActionAvro userActionAvro) {
        UserAction userAction = new UserAction();
        userAction.setUserId(userActionAvro.getUserId());
        userAction.setEventId(userActionAvro.getEventId());
        userAction.setActionType(convertActionTypeAvroToActionType(userActionAvro.getActionType()));
        userAction.setTimestamp(userActionAvro.getTimestamp());
        userActionRepository.save(userAction);
    }

    private void updateUserAction(UserAction userAction, UserActionAvro userActionAvro) {
        Double rateUserAction = convertActionTypeToDouble(userAction.getActionType());
        Double rateUserActionAvro = convertActionTypeAvroToDouble(userActionAvro.getActionType());
        if (rateUserActionAvro > rateUserAction) {
            userAction.setActionType(
                    convertActionTypeAvroToActionType(userActionAvro.getActionType()));
            userAction.setTimestamp(userActionAvro.getTimestamp());
            userActionRepository.save(userAction);
        }
    }

    private ActionType convertActionTypeAvroToActionType(ActionTypeAvro actionTypeAvro) {
        switch (actionTypeAvro) {
            case VIEW -> {
                return ActionType.VIEW;
            }
            case REGISTER -> {
                return ActionType.REGISTER;
            }
            case LIKE -> {
                return ActionType.LIKE;
            }
            default ->
                    throw new ActionTypeNotFound(
                            "Action type " + actionTypeAvro.name() + " not found");
        }
    }

    private Double convertActionTypeAvroToDouble(ActionTypeAvro actionTypeAvro) {
        switch (actionTypeAvro) {
            case VIEW -> {
                return VIEWING_RATE;
            }
            case REGISTER -> {
                return REGISTRATION_RATE;
            }
            case LIKE -> {
                return LIKE_RATE;
            }
            default ->
                    throw new ActionTypeNotFound(
                            "Action type " + actionTypeAvro.name() + " not found");
        }
    }

    private Double convertActionTypeToDouble(ActionType actionType) {
        switch (actionType) {
            case VIEW -> {
                return VIEWING_RATE;
            }
            case REGISTER -> {
                return REGISTRATION_RATE;
            }
            case LIKE -> {
                return LIKE_RATE;
            }
            default ->
                    throw new ActionTypeNotFound("Action type " + actionType.name() + " not found");
        }
    }
}
