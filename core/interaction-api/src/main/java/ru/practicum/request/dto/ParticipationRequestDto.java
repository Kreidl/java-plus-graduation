package ru.practicum.request.dto;

import java.time.LocalDateTime;

public record ParticipationRequestDto(
        LocalDateTime created, Long eventId, Long id, Long requesterId, String status) {}
