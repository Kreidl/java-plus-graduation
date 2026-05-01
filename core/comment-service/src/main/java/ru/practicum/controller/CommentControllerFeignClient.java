package ru.practicum.controller;

import java.util.Collection;
import java.util.List;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.CommentsPrivateGetRequest;
import ru.practicum.comment.dto.EventCommentCount;
import ru.practicum.feign.CommentFeign;
import ru.practicum.service.CommentService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comment")
public class CommentControllerFeignClient implements CommentFeign {
    private final CommentService commentService;

    @Override
    public Collection<CommentDto> getAllCommentsPaged(Long userId, int from, int size) {
        log.info("API get all comments requested by userId:{}", userId);
        CommentsPrivateGetRequest request = new CommentsPrivateGetRequest(userId, from, size);
        return commentService.getAllCommentsOfUserPaged(request);
    }

    @Override
    public List<EventCommentCount> countCommentsByEventIds(List<Long> eventIds) {
        log.info("API get count comments to eventIds:{}", eventIds);
        return commentService.countCommentsByEventIds(eventIds);
    }
}
