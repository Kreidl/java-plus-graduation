package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.event.controller.EventSortBy;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record EventsPublicGetRequest(
        String text,
        List<Long> categories,
        Boolean paid,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        boolean onlyAvailable,
        EventSortBy sort,
        int from,
        int size,
        HttpServletRequest httpRequest) {

    public boolean hasText() {
        return text != null && !text.isEmpty();
    }

    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }

    public boolean hasPaid() {
        return paid != null;
    }

    public boolean hasRangeStart() {
        return rangeStart != null;
    }

    public boolean hasRangeEnd() {
        return rangeEnd != null;
    }

    public boolean hasSortBy() {
        return sort != null;
    }

    public Pageable getPageable() {
        int page = from / size;
        if (EventSortBy.EVENT_DATE.equals(sort)) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventDate"));
        }
        return PageRequest.of(page, size);
    }
}
