package ru.practicum.event.dto;

import java.time.LocalDateTime;

public interface UpdatableEvent {
    boolean hasAnnotation();

    String annotation();

    boolean hasEventDate();

    LocalDateTime eventDate();

    boolean hasCategory();

    boolean hasLocation();

    LocationDto location();

    boolean hasParticipantLimit();

    Integer participantLimit();

    boolean hasPaid();

    Boolean paid();

    boolean hasRequestModeration();

    Boolean requestModeration();

    boolean hasTitle();

    String title();

    boolean hasDescription();

    String description();
}
