package ru.practicum.event.controller;

import ru.practicum.event.dto.EventCheckDto;
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
    public EventCheckDto getEventCheckDtoById(Long eventId) {
        log.info("API get event requested to eventId:{}", eventId);
        return eventService.getEventCheckDtoById(eventId);
    }

    @Override
    public Boolean existsById(Long eventId) {
        log.info("API existing event requested to eventId:{}", eventId);
        return eventService.existsById(eventId);
    }

    @Override
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests) {
        log.info(
                "API update confirmed requests count to {} requested to eventId:{}",
                confirmedRequests,
                eventId);
        eventService.updateConfirmedRequests(eventId, confirmedRequests);
    }
}
