package ru.practicum.event.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import ru.practicum.event.model.UserStateAction;

public record UpdateEventUserRequest(
        @Size(min = 20, max = 2000) String annotation,
        Long category,
        @Size(min = 20, max = 7000) String description,
        @Future LocalDateTime eventDate,
        LocationDto location,
        Boolean paid,
        @PositiveOrZero Integer participantLimit,
        Boolean requestModeration,
        UserStateAction stateAction,
        @Size(min = 3, max = 120) String title)
        implements UpdatableEvent {

    public boolean hasAnnotation() {
        return annotation != null && !annotation.isBlank();
    }

    public boolean hasCategory() {
        return category != null;
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasEventDate() {
        return eventDate != null;
    }

    public boolean hasLocation() {
        return location != null;
    }

    public boolean hasPaid() {
        return paid != null;
    }

    public boolean hasParticipantLimit() {
        return participantLimit != null;
    }

    public boolean hasRequestModeration() {
        return requestModeration != null;
    }

    public boolean hasStateAction() {
        return stateAction != null;
    }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }
}
