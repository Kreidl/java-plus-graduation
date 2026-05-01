package ru.practicum.feign;

import ru.practicum.event.dto.EventFullDto;
import ru.practicum.feign.fallback.EventFeignFallback;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import feign.FeignException;

@FeignClient(name = "event-service", path = "/api/v1/event", fallback = EventFeignFallback.class)
public interface EventFeign {
    @GetMapping("/events/{eventId}")
    EventFullDto getEventFullDtoById(@PathVariable Long eventId) throws FeignException;

    @GetMapping("/events/{eventId}/exists")
    public Boolean existsById(@PathVariable Long eventId) throws FeignException;
}
