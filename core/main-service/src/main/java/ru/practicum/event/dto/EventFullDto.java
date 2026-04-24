package ru.practicum.event.dto;

import java.time.LocalDateTime;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.model.EventState;
import ru.practicum.user.dto.UserShortDto;

public record EventFullDto(
        String annotation,
        CategoryDto category,
        Long confirmedRequests,
        LocalDateTime createdOn,
        String description,
        LocalDateTime eventDate,
        Long id,
        UserShortDto initiator,
        LocationDto location,
        boolean paid,
        Integer participantLimit,
        LocalDateTime publishedOn,
        boolean requestModeration,
        EventState state,
        String title,
        Long views,
        Long commentaries) {}
