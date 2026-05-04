package ru.practicum.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ru.practicum.comment.dto.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.exception.ForbiddenAccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventFeign;
import ru.practicum.feign.UserFeign;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.repository.CommentRepository;
import ru.practicum.user.dto.UserShortDto;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserFeign userFeign;
    private final EventFeign eventFeign;

    @Override
    @Transactional(readOnly = true)
    public Collection<CommentDto> getAllCommentsPaged(CommentsPublicGetRequest request) {
        if (!eventFeign.existsById(request.eventId())) {
            throw new NotFoundException("Event with id=%d not found".formatted(request.eventId()));
        }

        Page<Comment> comments =
                commentRepository.findAllByEventId(request.eventId(), request.getPageable());

        return comments.stream()
                .map(comment -> CommentMapper.toCommentDto(comment, comment.getAuthorId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CommentDto> getAllCommentsOfUserPaged(CommentsPrivateGetRequest request) {
        UserShortDto user = userFeign.getUserShortById(request.userId());

        List<Comment> comments =
                commentRepository.findAllByAuthorId(request.userId(), request.getPageable());

        return comments.stream()
                .map(comment -> CommentMapper.toCommentDto(comment, user.id()))
                .toList();
    }

    @Override
    public void deleteComment(long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Override
    public void deleteCommentByUser(long userId, long commentId) {
        UserShortDto user = userFeign.getUserShortById(userId);
        Comment comment = getCommentByIdOrThrow(commentId);
        if (!comment.getAuthorId().equals(user.id())) {
            throw new ForbiddenAccessException("You are not allowed to delete others comments");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public CommentDto createComment(CommentsCreateRequest request) {
        EventFullDto event = eventFeign.getEventFullDtoById(request.newComment().eventId());
        UserShortDto user = userFeign.getUserShortById(request.userId());

        Comment newComment = CommentMapper.toEntity(request.newComment(), user.id(), event.id());
        Comment saved = commentRepository.save(newComment);
        return CommentMapper.toCommentDto(saved, user.id());
    }

    @Override
    public CommentDto updateComment(CommentsUpdateRequest request) {
        UserShortDto user = userFeign.getUserShortById(request.userId());
        Comment comment = getCommentByIdOrThrow(request.commentId());
        if (!comment.getAuthorId().equals(user.id())) {
            throw new ForbiddenAccessException("You are not allowed to update others comments");
        }

        comment.setText(request.updateComment().text());
        comment.setEdited(true);
        Comment saved = commentRepository.save(comment);
        return CommentMapper.toCommentDto(saved, user.id());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getById(Long commentId) {
        Comment comment = getCommentByIdOrThrow(commentId);
        return CommentMapper.toCommentDto(comment, comment.getAuthorId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCommentCount> countCommentsByEventIds(List<Long> eventIds) {
        return commentRepository.countCommentsByEventIds(eventIds);
    }

    private Comment getCommentByIdOrThrow(Long commentId) {
        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        return optionalComment.orElseThrow(
                NotFoundException.supplier("Comment with id=%d not found", commentId));
    }
}
