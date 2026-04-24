package ru.practicum.compilation.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

public record UpdateCompilationRequest(
        List<Long> events, Boolean pinned, @Size(min = 1, max = 50) String title) {

    public boolean hasEvents() {
        return events != null;
    }

    public boolean hasPinned() {
        return pinned != null;
    }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }
}
