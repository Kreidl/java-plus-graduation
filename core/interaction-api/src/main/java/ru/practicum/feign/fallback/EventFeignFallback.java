package ru.practicum.feign.fallback;

import ru.practicum.event.dto.EventCheckDto;
import ru.practicum.feign.EventFeign;

import org.springframework.stereotype.Component;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EventFeignFallback implements EventFeign {
    @Override
    public EventCheckDto getEventCheckDtoById(Long eventId) throws FeignException {
        fallback();
        return null;
    }

    @Override
    public Boolean existsById(Long eventId) throws FeignException {
        fallback();
        return null;
    }

    @Override
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests)
            throws FeignException {
        fallback();
    }

    private void fallback() {
        log.error("Fallback: event service is not responding");
        throw new RuntimeException("Event service is not responding, please try again later");
    }
}
