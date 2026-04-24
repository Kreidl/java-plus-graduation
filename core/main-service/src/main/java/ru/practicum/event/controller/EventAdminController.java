package ru.practicum.event.controller;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.model.EventState;
import ru.practicum.event.service.EventService;
import ru.practicum.event.service.EventsAdminGetRequest;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class EventAdminController {
    private final EventService eventService;

    @GetMapping
    public Collection<EventFullDto> getEventsFiltered(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) LocalDateTime rangeStart,
            @RequestParam(required = false) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        EventsAdminGetRequest getRequest =
                new EventsAdminGetRequest(
                        users, states, categories, rangeStart, rangeEnd, from, size);
        log.info("Admin get events requested with params= {}", getRequest);
        return eventService.getEvents(getRequest);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable Long eventId, @RequestBody @Valid UpdateEventAdminRequest updateRequest) {
        log.info("Admin update event requested with body= {}", updateRequest);
        return eventService.updateEvent(eventId, updateRequest);
    }
}
