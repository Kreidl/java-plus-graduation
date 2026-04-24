package ru.practicum.compilation.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record CompilationsPublicGetRequest(Boolean pinned, int from, int size) {

    public CompilationsPublicGetRequest {
        if (pinned == null) pinned = false;
    }

    public Pageable getPageable() {
        int page = from / size;
        return PageRequest.of(page, size);
    }
}
