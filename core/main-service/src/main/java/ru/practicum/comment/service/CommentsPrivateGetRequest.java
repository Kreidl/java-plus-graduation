package ru.practicum.comment.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record CommentsPrivateGetRequest(long userId, int from, int size) {
    public Pageable getPageable() {
        int page = from / size;
        return PageRequest.of(page, size);
    }
}
