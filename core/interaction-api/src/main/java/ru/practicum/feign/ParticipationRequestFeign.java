package ru.practicum.feign;

import java.util.Collection;
import java.util.Map;

import ru.practicum.feign.fallback.ParticipationRequestFeignFallback;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.FeignException;

@FeignClient(
        name = "request-service",
        path = "/api/v1/request",
        fallback = ParticipationRequestFeignFallback.class)
public interface ParticipationRequestFeign {
    @PostMapping("/confirmed")
    Map<Long, Long> countConfirmedByEventIds(@RequestBody Collection<Long> eventIds)
            throws FeignException;
}
