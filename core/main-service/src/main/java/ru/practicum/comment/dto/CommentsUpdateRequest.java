package ru.practicum.comment.dto;

public record CommentsUpdateRequest(long userId, long commentId, UpdateCommentDto updateComment) {}
