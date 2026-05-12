package ru.practicum.feign.fallback;

import java.util.Collection;
import java.util.List;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.EventCommentCount;
import ru.practicum.feign.CommentFeign;

import org.springframework.stereotype.Component;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommentFeignFallback implements CommentFeign {
    @Override
    public Collection<CommentDto> getAllCommentsPaged(Long userId, int from, int size) {
        fallback();
        return List.of();
    }

    @Override
    public List<EventCommentCount> countCommentsByEventIds(List<Long> eventIds)
            throws FeignException {
        fallback();
        return List.of();
    }

    private void fallback() {
        log.error("Fallback: comment service is not responding");
        throw new RuntimeException("Comment service is not responding, please try again later");
    }
}
