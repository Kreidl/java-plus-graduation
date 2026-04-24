package ru.practicum.event.service;

import java.time.LocalDateTime;
import java.util.List;

import ru.practicum.event.model.EventState;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record EventsAdminGetRequest(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        int from,
        int size) {

    public boolean hasUsers() {
        return users != null && !users.isEmpty();
    }

    public boolean hasStates() {
        return states != null && !states.isEmpty();
    }

    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }

    public boolean hasRangeStart() {
        return rangeStart != null;
    }

    public boolean hasRangeEnd() {
        return rangeEnd != null;
    }

    public Pageable getPageable() {
        int page = from / size;
        return PageRequest.of(page, size);
    }
}
