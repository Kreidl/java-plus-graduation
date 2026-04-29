package ru.practicum.event.dto;

import java.time.LocalDateTime;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;

public record EventShortDto(
        String annotation,
        CategoryDto category,
        Long confirmedRequests,
        LocalDateTime eventDate,
        Long id,
        UserShortDto initiator,
        boolean paid,
        String title,
        Long views,
        Long commentaries) {}
