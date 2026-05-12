package ru.practicum.exception.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        List<String> errors,
        String message,
        String reason,
        String status,
        LocalDateTime timestamp) {}
