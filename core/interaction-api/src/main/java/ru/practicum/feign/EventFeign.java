package ru.practicum.feign;

import ru.practicum.event.dto.EventCheckDto;
import ru.practicum.feign.fallback.EventFeignFallback;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import feign.FeignException;

@FeignClient(name = "event-service", path = "/api/v1/event", fallback = EventFeignFallback.class)
public interface EventFeign {
    @GetMapping("/events/{eventId}")
    EventCheckDto getEventCheckDtoById(@PathVariable Long eventId) throws FeignException;

    @GetMapping("/events/{eventId}/exists")
    Boolean existsById(@PathVariable Long eventId) throws FeignException;

    @PatchMapping("/events/{eventId}/requests/confirmed")
    void updateConfirmedRequests(@PathVariable Long eventId, @RequestBody Long confirmedRequests)
            throws FeignException;
}
