package ru.practicum.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

public interface StatService {
    void createEndpointHit(EndpointHitDto endpointHitDto);

    Collection<ViewStatsDto> getStat(
            LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}
