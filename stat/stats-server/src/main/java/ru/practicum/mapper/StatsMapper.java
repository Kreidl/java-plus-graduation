package ru.practicum.mapper;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.model.Stats;

public class StatsMapper {
    public static Stats mapToEntity(EndpointHitDto endpointHitDto) {
        return new Stats(
                null,
                endpointHitDto.app(),
                endpointHitDto.uri(),
                endpointHitDto.ip(),
                endpointHitDto.timestamp());
    }
}
