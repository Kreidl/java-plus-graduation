package ru.practicum.comment.controller;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.service.*;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
public class CommentPrivateController {
    private final CommentService commentService;

    @GetMapping
    public Collection<CommentDto> getAllCommentsPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Private get all comments requested by userId:{}", userId);
        CommentsPrivateGetRequest request = new CommentsPrivateGetRequest(userId, from, size);
        return commentService.getAllCommentsOfUserPaged(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(
            @PathVariable long userId, @Valid @RequestBody NewCommentDto newComment) {
        CommentsCreateRequest request = new CommentsCreateRequest(userId, newComment);
        log.info("Create new comment requested");
        return commentService.createComment(request);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(
            @PathVariable long userId,
            @PathVariable long commentId,
            @Valid @RequestBody UpdateCommentDto updateComment) {

        CommentsUpdateRequest request = new CommentsUpdateRequest(userId, commentId, updateComment);

        log.info("Update comment requested by userId:{}, commentId:{}", userId, commentId);

        return commentService.updateComment(request);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable long userId, @PathVariable long commentId) {

        log.info("Delete comment requested by userId:{}, commentId:{}", userId, commentId);

        commentService.deleteCommentByUser(userId, commentId);
    }
}
