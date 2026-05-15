package ru.practicum.event.service;

import java.util.Collection;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.event.dto.*;

public interface EventService {
    EventFullDto getById(Long eventId, Long userId, HttpServletRequest request);

    Collection<EventShortDto> getEvents(EventsPublicGetRequest getRequest);

    Collection<EventFullDto> getEvents(EventsAdminGetRequest getRequest);

    Collection<EventShortDto> getEvents(EventsPrivateGetRequest getRequest);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getByUserById(Long userId, Long eventId);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    EventCheckDto getEventCheckDtoById(Long eventId);

    Boolean existsById(Long eventId);

    void updateConfirmedRequests(Long eventId, Long confirmedRequests);

    List<EventShortDto> getRecommendations(long userId, int maxResult);

    void addLike(Long eventId, long userId);
}
