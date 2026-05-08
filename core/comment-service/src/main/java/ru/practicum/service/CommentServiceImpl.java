package ru.practicum.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ru.practicum.comment.dto.*;
import ru.practicum.event.dto.EventCheckDto;
import ru.practicum.exception.ConflictException;
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

import feign.FeignException;
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
        log.info(
                "Fetching public comments for eventId={}, from={}, size={}",
                request.eventId(),
                request.getPageable().getPageNumber(),
                request.getPageable().getPageSize());

        if (!eventFeign.existsById(request.eventId())) {
            log.warn("Event with id={} not found when fetching comments", request.eventId());
            throw new NotFoundException("Event with id=%d not found".formatted(request.eventId()));
        }

        Page<Comment> comments =
                commentRepository.findAllByEventId(request.eventId(), request.getPageable());
        log.debug(
                "Found {} comments for eventId={}", comments.getTotalElements(), request.eventId());

        return comments.stream()
                .map(comment -> CommentMapper.toCommentDto(comment, comment.getAuthorId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CommentDto> getAllCommentsOfUserPaged(CommentsPrivateGetRequest request) {
        log.info(
                "Fetching private comments for userId={}, from={}, size={}",
                request.userId(),
                request.getPageable().getPageNumber(),
                request.getPageable().getPageSize());

        UserShortDto user = userFeign.getUserShortById(request.userId());
        log.debug("Fetched user info for userId={}", request.userId());

        List<Comment> comments =
                commentRepository.findAllByAuthorId(request.userId(), request.getPageable());
        log.debug("Found {} comments for userId={}", comments.size(), request.userId());

        return comments.stream()
                .map(comment -> CommentMapper.toCommentDto(comment, user.id()))
                .toList();
    }

    @Override
    public void deleteComment(long commentId) {
        log.info("Deleting comment with id={} (admin operation)", commentId);
        commentRepository.deleteById(commentId);
        log.debug("Comment with id={} deleted successfully", commentId);
    }

    @Override
    public void deleteCommentByUser(long userId, long commentId) {
        log.info("User {} requested to delete comment with id={}", userId, commentId);

        UserShortDto user = userFeign.getUserShortById(userId);
        Comment comment = getCommentByIdOrThrow(commentId);

        if (!comment.getAuthorId().equals(user.id())) {
            log.warn(
                    "User {} attempted to delete comment {} owned by user {}",
                    userId,
                    commentId,
                    comment.getAuthorId());
            throw new ForbiddenAccessException("You are not allowed to delete others comments");
        }

        commentRepository.deleteById(commentId);
        log.info("Comment with id={} deleted successfully by user {}", commentId, userId);
    }

    @Override
    public CommentDto createComment(CommentsCreateRequest request) {
        log.info(
                "Creating new comment for eventId={} by userId={}",
                request.newComment().eventId(),
                request.userId());

        EventCheckDto event = getEventOrThrowConflict(request.newComment().eventId());
        log.debug("Event validation passed for eventId={}", request.newComment().eventId());

        UserShortDto user = userFeign.getUserShortById(request.userId());
        log.debug("User validation passed for userId={}", request.userId());

        Comment newComment = CommentMapper.toEntity(request.newComment(), user.id(), event.id());
        Comment saved = commentRepository.save(newComment);

        log.info(
                "Comment created successfully with id={} for eventId={} by userId={}",
                saved.getId(),
                event.id(),
                user.id());

        return CommentMapper.toCommentDto(saved, user.id());
    }

    @Override
    public CommentDto updateComment(CommentsUpdateRequest request) {
        log.info("Updating comment with id={} by userId={}", request.commentId(), request.userId());

        UserShortDto user = userFeign.getUserShortById(request.userId());
        Comment comment = getCommentByIdOrThrow(request.commentId());

        if (!comment.getAuthorId().equals(user.id())) {
            log.warn(
                    "User {} attempted to update comment {} owned by user {}",
                    request.userId(),
                    request.commentId(),
                    comment.getAuthorId());
            throw new ForbiddenAccessException("You are not allowed to update others comments");
        }

        comment.setText(request.updateComment().text());
        comment.setEdited(true);
        Comment saved = commentRepository.save(comment);

        log.info(
                "Comment with id={} updated successfully by userId={}",
                comment.getId(),
                request.userId());
        return CommentMapper.toCommentDto(saved, user.id());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getById(Long commentId) {
        log.debug("Fetching comment by id={}", commentId);
        Comment comment = getCommentByIdOrThrow(commentId);
        return CommentMapper.toCommentDto(comment, comment.getAuthorId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCommentCount> countCommentsByEventIds(List<Long> eventIds) {
        log.debug("Counting comments for eventIds: {}", eventIds);
        List<EventCommentCount> counts = commentRepository.countCommentsByEventIds(eventIds);
        log.debug("Found comment counts for {} events", counts.size());
        return counts;
    }

    private Comment getCommentByIdOrThrow(Long commentId) {
        log.debug("Looking up comment with id={}", commentId);
        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        Comment comment =
                optionalComment.orElseThrow(
                        NotFoundException.supplier("Comment with id=%d not found", commentId));
        log.debug("Found comment with id={}", commentId);
        return comment;
    }

    private EventCheckDto getEventOrThrowConflict(Long eventId) {
        log.debug("Fetching event check data for eventId={} via Feign", eventId);
        try {
            EventCheckDto event = eventFeign.getEventCheckDtoById(eventId);
            log.debug("Successfully fetched event data for eventId={}", eventId);
            return event;
        } catch (FeignException.NotFound e) {
            log.debug("Event {} not found via Feign, converting to ConflictException", eventId);
            throw new ConflictException("Event with id=" + eventId + " not found or not published");
        } catch (FeignException e) {
            log.error("Failed to fetch event {} via Feign: {}", eventId, e.getMessage(), e);
            throw new ConflictException("Failed to validate event with id=" + eventId);
        }
    }
}
