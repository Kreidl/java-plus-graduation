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
        log.debug(
                "Processing UserActionAvro: userId={}, eventId={}, actionType={}",
                userActionAvro.getUserId(),
                userActionAvro.getEventId(),
                userActionAvro.getActionType());

        Optional<UserAction> userActionOpt =
                userActionRepository.findByUserIdAndEventId(
                        userActionAvro.getUserId(), userActionAvro.getEventId());

        if (userActionOpt.isEmpty()) {
            log.info(
                    "Creating new user action record: userId={}, eventId={}, actionType={}",
                    userActionAvro.getUserId(),
                    userActionAvro.getEventId(),
                    userActionAvro.getActionType());
            createUserAction(userActionAvro);
        } else {
            log.debug(
                    "Updating existing user action: userId={}, eventId={}, oldActionType={},"
                            + " newActionType={}",
                    userActionAvro.getUserId(),
                    userActionAvro.getEventId(),
                    userActionOpt.get().getActionType(),
                    userActionAvro.getActionType());
            updateUserAction(userActionOpt.get(), userActionAvro);
        }
    }

    private void createUserAction(UserActionAvro userActionAvro) {
        UserAction userAction = new UserAction();
        userAction.setUserId(userActionAvro.getUserId());
        userAction.setEventId(userActionAvro.getEventId());
        userAction.setActionType(convertActionTypeAvroToActionType(userActionAvro.getActionType()));
        userAction.setTimestamp(userActionAvro.getTimestamp());

        UserAction saved = userActionRepository.save(userAction);
        log.info(
                "User action created successfully: id={}, userId={}, eventId={}, actionType={}",
                saved.getId(),
                userActionAvro.getUserId(),
                userActionAvro.getEventId(),
                userActionAvro.getActionType());
    }

    private void updateUserAction(UserAction userAction, UserActionAvro userActionAvro) {
        Double rateUserAction = convertActionTypeToDouble(userAction.getActionType());
        Double rateUserActionAvro = convertActionTypeAvroToDouble(userActionAvro.getActionType());

        if (rateUserActionAvro > rateUserAction) {
            log.debug(
                    "Upgrading user action: userId={}, eventId={}, from {} (rate={}) to {}"
                            + " (rate={})",
                    userAction.getUserId(),
                    userAction.getEventId(),
                    userAction.getActionType(),
                    rateUserAction,
                    userActionAvro.getActionType(),
                    rateUserActionAvro);

            userAction.setActionType(
                    convertActionTypeAvroToActionType(userActionAvro.getActionType()));
            userAction.setTimestamp(userActionAvro.getTimestamp());

            UserAction updated = userActionRepository.save(userAction);
            log.info(
                    "User action updated successfully: id={}, userId={}, eventId={},"
                            + " newActionType={}",
                    updated.getId(),
                    userAction.getUserId(),
                    userAction.getEventId(),
                    userActionAvro.getActionType());
        } else {
            log.trace(
                    "No update needed: existing action {} (rate={}) has higher or equal weight than"
                            + " new action {} (rate={})",
                    userAction.getActionType(),
                    rateUserAction,
                    userActionAvro.getActionType(),
                    rateUserActionAvro);
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
            default -> {
                log.error("Unknown Avro action type: {}", actionTypeAvro);
                throw new ActionTypeNotFound("Action type " + actionTypeAvro.name() + " not found");
            }
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
            default -> {
                log.warn("Unknown action type for weight conversion: {}", actionTypeAvro);
                throw new ActionTypeNotFound("Action type " + actionTypeAvro.name() + " not found");
            }
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
            default -> {
                log.warn("Unknown action type for weight conversion: {}", actionType);
                throw new ActionTypeNotFound("Action type " + actionType.name() + " not found");
            }
        }
    }
}
