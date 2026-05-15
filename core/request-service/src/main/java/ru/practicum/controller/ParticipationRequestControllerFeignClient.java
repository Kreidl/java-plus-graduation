package ru.practicum.controller;

import java.util.Collection;
import java.util.Map;

import ru.practicum.feign.ParticipationRequestFeign;
import ru.practicum.service.ParticipationRequestService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/request")
public class ParticipationRequestControllerFeignClient implements ParticipationRequestFeign {
    private final ParticipationRequestService participationRequestService;

    @Override
    public Map<Long, Long> countConfirmedByEventIds(Collection<Long> eventIds) {
        log.info("API get count confirmed requests requested to eventIds:{}", eventIds);
        return participationRequestService.countConfirmedByEventIds(eventIds);
    }

    @Override
    public Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId) {
        return participationRequestService.existsByRequesterIdAndEventId(requesterId, eventId);
    }
}
