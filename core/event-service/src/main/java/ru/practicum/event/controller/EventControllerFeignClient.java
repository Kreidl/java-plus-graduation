package ru.practicum.event.controller;

import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.service.EventService;
import ru.practicum.feign.EventFeign;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/event")
public class EventControllerFeignClient implements EventFeign {
    private final EventService eventService;

    @Override
    public EventFullDto getEventFullDtoById(Long eventId) {
        log.info("API get event requested to eventId:{}", eventId);
        return eventService.getEventFullDtoById(eventId);
    }

    @Override
    public Boolean existsById(Long eventId) {
        log.info("API existing event requested to eventId:{}", eventId);
        return eventService.existsById(eventId);
    }
}
