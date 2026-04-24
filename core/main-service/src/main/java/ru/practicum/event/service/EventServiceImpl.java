package ru.practicum.event.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.StatsClient;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.repository.EventCommentCount;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.controller.EventSortBy;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.mapper.LocationMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ForbiddenAccessException;
import ru.practicum.exception.IllegalEventUpdateException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private static final Duration MIN_TIME_BEFORE_EVENT = Duration.ofHours(2);
    private static final String EVENTS_URI = "/events/%d";
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private final ParticipationRequestRepository requestRepository;
    private final CommentRepository commentRepository;

    @Override
    public EventFullDto getById(Long eventId, HttpServletRequest request) {
        Optional<Event> eventOptional =
                eventRepository.findByIdAndState(eventId, EventState.PUBLISHED);
        Event event =
                eventOptional.orElseThrow(
                        NotFoundException.supplier("Event with id=%d not found", eventId));

        statsClient.hit(request);
        String uri = request.getRequestURI();

        ViewStatsDto statsDto = getStatsForEvent(event, uri);

        Map<Long, Long> confirmedRequests = getConfirmedRequests(Set.of(event));

        Map<Long, Long> commentariesCount = getCommentariesCount(Set.of(event));

        return EventMapper.mapToFullDto(
                event,
                confirmedRequests.getOrDefault(event.getId(), 0L),
                statsDto.hits(),
                commentariesCount.getOrDefault(event.getId(), 0L));
    }

    @Override
    public Collection<EventShortDto> getEvents(EventsPublicGetRequest getRequest) {
        Page<Event> events =
                eventRepository.findAll(
                        EventRepository.createPredicate(getRequest), getRequest.getPageable());

        statsClient.hit(getRequest.httpRequest());

        Set<Event> eventsSet = events.stream().collect(Collectors.toSet());

        Map<Long, Long> statsForEvents = getStatsMapForEvents(events);

        Map<Long, Long> confirmedRequests = getConfirmedRequests(eventsSet);

        Map<Long, Long> commentariesCount = getCommentariesCount(eventsSet);

        List<EventShortDto> eventsList =
                events.stream()
                        .map(
                                event ->
                                        EventMapper.mapToShortDto(
                                                event,
                                                confirmedRequests.getOrDefault(event.getId(), 0L),
                                                statsForEvents.get(event.getId()),
                                                commentariesCount.getOrDefault(event.getId(), 0L)))
                        .toList();

        if (EventSortBy.VIEWS.equals(getRequest.sort())) {
            return eventsList.stream().sorted(Comparator.comparing(EventShortDto::views)).toList();
        }

        return eventsList;
    }

    @Override
    public Collection<EventFullDto> getEvents(EventsAdminGetRequest getRequest) {
        Page<Event> events =
                eventRepository.findAll(
                        EventRepository.createPredicate(getRequest), getRequest.getPageable());

        Map<Long, Long> statsForEvents = getStatsMapForEvents(events);

        Map<Long, Long> confirmedRequests =
                getConfirmedRequests(events.stream().collect(Collectors.toSet()));

        return events.stream()
                .map(
                        event ->
                                EventMapper.mapToFullDto(
                                        event,
                                        confirmedRequests.getOrDefault(event.getId(), 0L),
                                        statsForEvents.get(event.getId()),
                                        null))
                .toList();
    }

    @Override
    public Collection<EventShortDto> getEvents(EventsPrivateGetRequest getRequest) {
        getUserByIdOrThrow(getRequest.userId());
        Page<Event> events =
                eventRepository.findByInitiator_Id(getRequest.userId(), getRequest.getPageable());

        Map<Long, Long> statsForEvents = getStatsMapForEvents(events);

        Map<Long, Long> confirmedRequests =
                getConfirmedRequests(events.stream().collect(Collectors.toSet()));

        return events.stream()
                .map(
                        event ->
                                EventMapper.mapToShortDto(
                                        event,
                                        confirmedRequests.getOrDefault(event.getId(), 0L),
                                        statsForEvents.get(event.getId()),
                                        null))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        Location location = LocationMapper.mapToEntity(newEventDto.location());
        Category category = getCategoryByIdOrThrow(newEventDto.category());
        User initiator = getUserByIdOrThrow(userId);
        Event event = EventMapper.mapToEntity(newEventDto, category, initiator, location);

        LocalDateTime now = LocalDateTime.now();
        if (event.getEventDate().isBefore(now.plus(MIN_TIME_BEFORE_EVENT))) {
            throw new ValidationException(
                    "The event must be scheduled at least %d hours from now."
                            .formatted(MIN_TIME_BEFORE_EVENT.toHours()));
        }
        Event saved = eventRepository.save(event);

        return EventMapper.mapToFullDto(saved, 0, 0L, 0L);
    }

    @Override
    public EventFullDto getByUserById(Long userId, Long eventId) {
        User user = getUserByIdOrThrow(userId);

        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiator().getId().equals(user.getId())) {
            throw new ForbiddenAccessException("You can't view event that's not yours");
        }

        ViewStatsDto statsDto = getStatsForEvent(event, EVENTS_URI.formatted(eventId));

        Map<Long, Long> confirmedRequests = getConfirmedRequests(Set.of(event));

        return EventMapper.mapToFullDto(
                event, confirmedRequests.getOrDefault(event.getId(), 0L), statsDto.hits(), null);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = getEventByIdOrThrow(eventId);

        if ((event.getState().equals(EventState.PUBLISHED)
                        || event.getState().equals(EventState.CANCELED))
                && updateRequest.hasStateAction()) {
            throw new IllegalEventUpdateException(
                    "Forbidden to update event that already %s"
                            .formatted(event.getState().toString()));
        }

        Category newCategory = null;
        if (updateRequest.hasCategory()) {
            newCategory = getCategoryByIdOrThrow(updateRequest.category());
        }
        EventMapper.updateEventFromDto(event, updateRequest, newCategory);

        Event saved = eventRepository.save(event);

        Map<Long, Long> confirmedRequests = getConfirmedRequests(Set.of(event));

        return EventMapper.mapToFullDto(
                saved, confirmedRequests.getOrDefault(event.getId(), 0L), null, null);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(
            Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = getEventByIdOrThrow(eventId);
        User user = getUserByIdOrThrow(userId);

        if (!event.getInitiator().getId().equals(user.getId())) {
            throw new ForbiddenAccessException("You can't update event that's not yours");
        }

        if ((event.getState().equals(EventState.PUBLISHED)
                        || event.getState().equals(EventState.CANCELED))
                && !updateRequest.hasStateAction()) {
            throw new IllegalEventUpdateException(
                    "Forbidden to update event that already %s"
                            .formatted(event.getState().toString()));
        }

        Category newCategory = null;
        if (updateRequest.hasCategory()) {
            newCategory = getCategoryByIdOrThrow(updateRequest.category());
        }
        EventMapper.updateEventFromDto(event, updateRequest, newCategory);

        Event saved = eventRepository.save(event);

        Map<Long, Long> confirmedRequests = getConfirmedRequests(Set.of(event));

        return EventMapper.mapToFullDto(
                saved, confirmedRequests.getOrDefault(event.getId(), 0L), null, null);
    }

    private Map<Long, Long> getCommentariesCount(Set<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        return commentRepository.countCommentsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventCommentCount::eventId, EventCommentCount::count));
    }

    private Map<Long, Long> getStatsMapForEvents(Page<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> listOfUris =
                events.stream().map(event -> EVENTS_URI.formatted(event.getId())).toList();
        LocalDateTime minimalPublishDate =
                events.stream()
                        .map(Event::getPublishedOn)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.of(1000, 1, 1, 1, 1));

        return getStatsForEvents(listOfUris, minimalPublishDate).stream()
                .collect(
                        Collectors.toMap(
                                statsDto ->
                                        Long.valueOf(
                                                statsDto.uri()
                                                        .substring(
                                                                statsDto.uri().lastIndexOf('/')
                                                                        + 1)),
                                ViewStatsDto::hits));
    }

    private Map<Long, Long> getConfirmedRequests(Set<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        return requestRepository.countConfirmedByEventIds(eventIds);
    }

    private List<ViewStatsDto> getStatsForEvents(List<String> uris, LocalDateTime startDate) {
        try {
            return statsClient.getStats(startDate, LocalDateTime.now(), uris, true);
        } catch (RestClientException e) {
            log.error("Error during getting stats for events", e);
        }
        return List.of();
    }

    private ViewStatsDto getStatsForEvent(Event event, String uri) {
        if (event.getPublishedOn() == null) {
            return new ViewStatsDto(null, null, null);
        }

        LocalDateTime startDate = event.getPublishedOn().minusSeconds(10);
        LocalDateTime endDate = LocalDateTime.now().plusSeconds(10);
        ViewStatsDto statsDto;

        try {
            statsDto = statsClient.getStats(startDate, endDate, List.of(uri), true).getFirst();
        } catch (NoSuchElementException e) {
            log.trace("No stats for event with id={} found", event.getId());
            statsDto = new ViewStatsDto(null, null, 0L);
        } catch (RestClientException e) {
            log.error("Error during getting stats for event with id={}", event.getId(), e);
            statsDto = new ViewStatsDto(null, null, null);
        }
        return statsDto;
    }

    private Event getEventByIdOrThrow(Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        return eventOptional.orElseThrow(
                NotFoundException.supplier("Event with id=%d not found", eventId));
    }

    private User getUserByIdOrThrow(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        return optionalUser.orElseThrow(
                NotFoundException.supplier("User with id=%d not found", userId));
    }

    private Category getCategoryByIdOrThrow(Long categoryId) {
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);
        return optionalCategory.orElseThrow(
                NotFoundException.supplier("Category with id=%d not found", categoryId));
    }
}
