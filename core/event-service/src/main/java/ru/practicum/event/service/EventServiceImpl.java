package ru.practicum.event.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.StatsClient;
import ru.practicum.comment.dto.EventCommentCount;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.enums.EventSortBy;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.mapper.LocationMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ForbiddenAccessException;
import ru.practicum.exception.IllegalEventUpdateException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.feign.CommentFeign;
import ru.practicum.feign.UserFeign;
import ru.practicum.user.dto.UserShortDto;

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
    private final UserFeign userFeign;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private final CommentFeign commentFeign;

    @Override
    public EventFullDto getById(Long eventId, HttpServletRequest request) {
        log.info("Fetching public event details for eventId={}", eventId);

        Optional<Event> eventOptional =
                eventRepository.findByIdAndState(eventId, EventState.PUBLISHED);
        Event event =
                eventOptional.orElseThrow(
                        NotFoundException.supplier("Event with id=%d not found", eventId));

        statsClient.hit(request);
        log.debug("Recorded hit for event {} at URI {}", eventId, request.getRequestURI());

        ViewStatsDto statsDto = getStatsForEvent(event, request.getRequestURI());
        Map<Long, Long> commentariesCount = getCommentariesCount(Set.of(event));
        UserShortDto userShortDto = userFeign.getUserShortById(event.getInitiatorId());

        log.debug("Assembled full event DTO for eventId={}", eventId);
        return EventMapper.mapToFullDto(
                event,
                statsDto.hits(),
                commentariesCount.getOrDefault(event.getId(), 0L),
                userShortDto);
    }

    @Override
    public EventCheckDto getEventCheckDtoById(Long eventId) {
        log.debug("Fetching event check data for eventId={}", eventId);

        Optional<Event> eventOptional =
                eventRepository.findByIdAndState(eventId, EventState.PUBLISHED);
        Event event =
                eventOptional.orElseThrow(
                        NotFoundException.supplier("Event with id=%d not found", eventId));

        Map<Long, Long> commentariesCount = getCommentariesCount(Set.of(event));
        UserShortDto userShortDto = userFeign.getUserShortById(event.getInitiatorId());

        return EventMapper.mapToCheckDto(
                event, commentariesCount.getOrDefault(event.getId(), 0L), userShortDto);
    }

    @Override
    public Boolean existsById(Long eventId) {
        log.trace("Checking existence of event with id={}", eventId);
        return eventRepository.existsById(eventId);
    }

    @Override
    public Collection<EventShortDto> getEvents(EventsPublicGetRequest getRequest) {
        log.info(
                "Fetching public events: filters={}, from={}, size={}, sort={}",
                getRequest,
                getRequest.getPageable().getPageNumber(),
                getRequest.getPageable().getPageSize(),
                getRequest.sort());

        Page<Event> events =
                eventRepository.findAll(
                        EventRepository.createPredicate(getRequest), getRequest.getPageable());
        log.debug("Found {} events matching criteria", events.getTotalElements());

        statsClient.hit(getRequest.httpRequest());
        Set<Event> eventsSet = events.stream().collect(Collectors.toSet());

        Map<Long, Long> statsForEvents = getStatsMapForEvents(events);
        Map<Long, Long> commentariesCount = getCommentariesCount(eventsSet);

        List<EventShortDto> eventsList =
                events.stream()
                        .map(
                                event ->
                                        EventMapper.mapToShortDto(
                                                event,
                                                statsForEvents.get(event.getId()),
                                                commentariesCount.getOrDefault(event.getId(), 0L),
                                                userFeign.getUserShortById(event.getInitiatorId())))
                        .toList();

        if (EventSortBy.VIEWS.equals(getRequest.sort())) {
            log.debug("Sorting {} events by views", eventsList.size());
            return eventsList.stream().sorted(Comparator.comparing(EventShortDto::views)).toList();
        }

        return eventsList;
    }

    @Override
    public Collection<EventFullDto> getEvents(EventsAdminGetRequest getRequest) {
        log.info(
                "Fetching admin events: filters={}, from={}, size={}",
                getRequest,
                getRequest.getPageable().getPageNumber(),
                getRequest.getPageable().getPageSize());

        Page<Event> events =
                eventRepository.findAll(
                        EventRepository.createPredicate(getRequest), getRequest.getPageable());
        log.debug("Found {} events for admin view", events.getTotalElements());

        Map<Long, Long> statsForEvents = getStatsMapForEvents(events);

        return events.stream()
                .map(
                        event ->
                                EventMapper.mapToFullDto(
                                        event,
                                        statsForEvents.get(event.getId()),
                                        null,
                                        userFeign.getUserShortById(event.getInitiatorId())))
                .toList();
    }

    @Override
    public Collection<EventShortDto> getEvents(EventsPrivateGetRequest getRequest) {
        log.info(
                "Fetching private events for userId={}, from={}, size={}",
                getRequest.userId(),
                getRequest.getPageable().getPageNumber(),
                getRequest.getPageable().getPageSize());

        UserShortDto userShortDto = userFeign.getUserShortById(getRequest.userId());
        Page<Event> events =
                eventRepository.findByInitiatorId(getRequest.userId(), getRequest.getPageable());
        log.debug("Found {} events for userId={}", events.getTotalElements(), getRequest.userId());

        Map<Long, Long> statsForEvents = getStatsMapForEvents(events);

        return events.stream()
                .map(
                        event ->
                                EventMapper.mapToShortDto(
                                        event,
                                        statsForEvents.get(event.getId()),
                                        null,
                                        userShortDto))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Creating new event for userId={}, title='{}'", userId, newEventDto.title());

        Location location = LocationMapper.mapToEntity(newEventDto.location());
        Category category = getCategoryByIdOrThrow(newEventDto.category());
        UserShortDto initiator = userFeign.getUserShortById(userId);
        Event event = EventMapper.mapToEntity(newEventDto, category, initiator.id(), location);

        LocalDateTime now = LocalDateTime.now();
        if (event.getEventDate().isBefore(now.plus(MIN_TIME_BEFORE_EVENT))) {
            log.warn(
                    "Event date {} is too close to now {} (min gap: {} hours)",
                    event.getEventDate(),
                    now,
                    MIN_TIME_BEFORE_EVENT.toHours());
            throw new ValidationException(
                    "The event must be scheduled at least %d hours from now."
                            .formatted(MIN_TIME_BEFORE_EVENT.toHours()));
        }

        Event saved = eventRepository.save(event);
        log.info("Event created successfully with id={}", saved.getId());

        return EventMapper.mapToFullDto(
                saved, 0L, 0L, userFeign.getUserShortById(event.getInitiatorId()));
    }

    @Override
    public EventFullDto getByUserById(Long userId, Long eventId) {
        log.info("Fetching event {} for user {}", eventId, userId);

        UserShortDto user = userFeign.getUserShortById(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiatorId().equals(user.id())) {
            log.warn(
                    "User {} attempted to access event {} owned by user {}",
                    userId,
                    eventId,
                    event.getInitiatorId());
            throw new ForbiddenAccessException("You can't view event that's not yours");
        }

        ViewStatsDto statsDto = getStatsForEvent(event, EVENTS_URI.formatted(eventId));
        log.debug("Assembled full event DTO for user view");

        return EventMapper.mapToFullDto(event, statsDto.hits(), null, user);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Admin updating event {} with changes: {}", eventId, updateRequest);

        Event event = getEventByIdOrThrow(eventId);

        if ((event.getState().equals(EventState.PUBLISHED)
                        || event.getState().equals(EventState.CANCELED))
                && updateRequest.hasStateAction()) {
            log.warn(
                    "Cannot change state of event {} which is already {}",
                    eventId,
                    event.getState());
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
        log.info("Event {} updated successfully by admin", eventId);

        return EventMapper.mapToFullDto(
                saved, null, null, userFeign.getUserShortById(event.getInitiatorId()));
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(
            Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("User {} updating their event {}", userId, eventId);

        Event event = getEventByIdOrThrow(eventId);
        UserShortDto user = userFeign.getUserShortById(userId);

        if (!event.getInitiatorId().equals(user.id())) {
            log.warn(
                    "User {} attempted to update event {} owned by user {}",
                    userId,
                    eventId,
                    event.getInitiatorId());
            throw new ForbiddenAccessException("You can't update event that's not yours");
        }

        if ((event.getState().equals(EventState.PUBLISHED)
                        || event.getState().equals(EventState.CANCELED))
                && !updateRequest.hasStateAction()) {
            log.warn("Cannot update event {} which is already {}", eventId, event.getState());
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
        log.info("Event {} updated successfully by user {}", eventId, userId);

        return EventMapper.mapToFullDto(saved, null, null, user);
    }

    @Override
    @Transactional
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests) {
        log.debug("Updating confirmed requests count for event {}: {}", eventId, confirmedRequests);
        Event event = getEventByIdOrThrow(eventId);
        event.setConfirmedRequests(confirmedRequests);
        eventRepository.save(event);
    }

    private Map<Long, Long> getCommentariesCount(Set<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        log.debug("Fetching comment counts for {} events", eventIds.size());

        return commentFeign.countCommentsByEventIds(eventIds).stream()
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

        log.debug(
                "Fetching stats for {} event URIs starting from {}",
                listOfUris.size(),
                minimalPublishDate);

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

    private List<ViewStatsDto> getStatsForEvents(List<String> uris, LocalDateTime startDate) {
        try {
            return statsClient.getStats(startDate, LocalDateTime.now(), uris, true);
        } catch (RestClientException e) {
            log.error("Error fetching stats for events: {}", e.getMessage(), e);
        }
        return List.of();
    }

    private ViewStatsDto getStatsForEvent(Event event, String uri) {
        if (event.getPublishedOn() == null) {
            return new ViewStatsDto(null, null, null);
        }

        LocalDateTime startDate = event.getPublishedOn().minusSeconds(10);
        LocalDateTime endDate = LocalDateTime.now().plusSeconds(10);

        try {
            return statsClient.getStats(startDate, endDate, List.of(uri), true).getFirst();
        } catch (NoSuchElementException e) {
            log.trace("No stats found for event {}", event.getId());
            return new ViewStatsDto(null, null, 0L);
        } catch (RestClientException e) {
            log.error("Error fetching stats for event {}", event.getId(), e);
            return new ViewStatsDto(null, null, null);
        }
    }

    private Event getEventByIdOrThrow(Long eventId) {
        log.debug("Looking up event with id={}", eventId);
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        return eventOptional.orElseThrow(
                NotFoundException.supplier("Event with id=%d not found", eventId));
    }

    private Category getCategoryByIdOrThrow(Long categoryId) {
        log.debug("Looking up category with id={}", categoryId);
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);
        return optionalCategory.orElseThrow(
                NotFoundException.supplier("Category with id=%d not found", categoryId));
    }
}
