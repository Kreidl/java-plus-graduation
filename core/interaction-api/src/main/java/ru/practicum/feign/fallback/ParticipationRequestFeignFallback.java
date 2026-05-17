package ru.practicum.feign.fallback;

import java.util.Collection;
import java.util.Map;

import ru.practicum.feign.ParticipationRequestFeign;

import org.springframework.stereotype.Component;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ParticipationRequestFeignFallback implements ParticipationRequestFeign {
    @Override
    public Map<Long, Long> countConfirmedByEventIds(Collection<Long> eventIds)
            throws FeignException {
        fallback();
        return Map.of();
    }

    @Override
    public Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId) {
        fallback();
        return null;
    }

    private void fallback() {
        log.error("Fallback: request service is not responding");
        throw new RuntimeException("Request service is not responding, please try again later");
    }
}
