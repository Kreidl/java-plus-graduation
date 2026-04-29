package ru.practicum.comment.dto;

import java.time.LocalDateTime;

import ru.practicum.user.dto.UserShortDto;

public record CommentDto(
        Long id, String text, UserShortDto author, LocalDateTime created, boolean edited) {}
