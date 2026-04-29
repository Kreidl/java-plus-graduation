package ru.practicum.service;

import static ru.practicum.mapper.StatsMapper.mapToEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.exception.IllegalArgumentException;
import ru.practicum.repository.StatRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {
    private final StatRepository statRepository;

    @Override
    @Transactional
    public void createEndpointHit(EndpointHitDto endpointHitDto) {
        statRepository.save(mapToEntity(endpointHitDto));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ViewStatsDto> getStat(
            LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("The end date must be before start date.");
        }

        if (uris != null) {
            if (unique) {
                return statRepository.getUniqueStatsWithUris(start, end, uris);
            } else {
                return statRepository.getNotUniqueStatsWithUris(start, end, uris);
            }
        } else {
            if (unique) {
                return statRepository.getUniqueStatsWithoutUris(start, end);
            } else {
                return statRepository.getNotUniqueStatsWithoutUris(start, end);
            }
        }
    }
}
