package ru.practicum.comment.dto;

public record CommentsCreateRequest(long userId, NewCommentDto newComment) {}
