package ru.practicum.comment.service;

import ru.practicum.comment.dto.UpdateCommentDto;

public record CommentsUpdateRequest(long userId, long commentId, UpdateCommentDto updateComment) {}
