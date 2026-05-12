package ru.practicum.comment.dto;

import java.time.LocalDateTime;

public record CommentDto(
        Long id, String text, Long authorId, LocalDateTime created, boolean edited) {}
