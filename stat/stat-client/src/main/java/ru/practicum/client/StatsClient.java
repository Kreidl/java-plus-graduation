package ru.practicum.client;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

public interface StatsClient {
    void hit(EndpointHitDto dto);

    void hit(HttpServletRequest request);

    List<ViewStatsDto> getStats(
            LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
