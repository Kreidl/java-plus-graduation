package ru.practicum.service;

import java.util.Collection;
import java.util.List;

import ru.practicum.comment.dto.*;

public interface CommentService {
    Collection<CommentDto> getAllCommentsPaged(CommentsPublicGetRequest request);

    Collection<CommentDto> getAllCommentsOfUserPaged(CommentsPrivateGetRequest request);

    void deleteComment(long commentId);

    void deleteCommentByUser(long userId, long commentId);

    CommentDto createComment(CommentsCreateRequest request);

    CommentDto updateComment(CommentsUpdateRequest request);

    CommentDto getById(Long commentId);

    List<EventCommentCount> countCommentsByEventIds(List<Long> eventIds);
}
