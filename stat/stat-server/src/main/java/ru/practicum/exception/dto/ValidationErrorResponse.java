package ru.practicum.exception.dto;

import java.util.List;

public record ValidationErrorResponse(List<Violation> violations) {}
