package ru.practicum.feign;

import java.util.Collection;
import java.util.List;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.EventCommentCount;
import ru.practicum.feign.fallback.CommentFeignFallback;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import feign.FeignException;

@Validated
@FeignClient(
        name = "comment-service",
        path = "/api/v1/comment",
        fallback = CommentFeignFallback.class)
public interface CommentFeign {
    @GetMapping("/users/{userId}/comments")
    public Collection<CommentDto> getAllCommentsPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size)
            throws FeignException;

    @GetMapping("/events/comments/count")
    public List<EventCommentCount> countCommentsByEventIds(List<Long> eventIds)
            throws FeignException;
}
