package ru.practicum.event.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.collector.CollectorGrpcClient;
import ru.practicum.client.recommendations.RecommendationsGrpcClient;
import ru.practicum.comment.dto.EventCommentCount;
import ru.practicum.event.dto.*;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.mapper.LocationMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.exception.ForbiddenAccessException;
import ru.practicum.exception.IllegalEventUpdateException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.feign.CommentFeign;
import ru.practicum.feign.ParticipationRequestFeign;
import ru.practicum.feign.UserFeign;
import ru.practicum.user.dto.UserShortDto;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.protobuf.Timestamp;

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
    private final CommentFeign commentFeign;
    private final ParticipationRequestFeign participationRequestFeign;
    private final CategoryRepository categoryRepository;
    private final RecommendationsGrpcClient recommendationsGrpcClient;
    private final CollectorGrpcClient collectorGrpcClient;

    @Override
    public EventFullDto getById(Long eventId, Long userId, HttpServletRequest request) {
        log.info("Fetching public event details for eventId={}", eventId);

        Optional<Event> eventOptional =
                eventRepository.findByIdAndState(eventId, EventState.PUBLISHED);
        Event event =
                eventOptional.orElseThrow(
                        NotFoundException.supplier("Event with id=%d not found", eventId));

        Double rating = getRatingForEvent(eventId);
        log.debug("Recorded rating for event {}, rating={}", eventId, rating);
        event.setRating(rating);
        Map<Long, Long> commentariesCount = getCommentariesCount(Set.of(event));
        UserShortDto userShortDto = userFeign.getUserShortById(event.getInitiatorId());

        log.debug("Assembled full event DTO for eventId={}", eventId);
        sendAction(eventId, userId, ActionTypeProto.ACTION_VIEW);
        return EventMapper.mapToFullDto(
                event, commentariesCount.getOrDefault(event.getId(), 0L), userShortDto);
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

        Set<Event> eventsSet = events.stream().collect(Collectors.toSet());

        Map<Long, Long> commentariesCount = getCommentariesCount(eventsSet);

        List<EventShortDto> eventsList =
                events.stream()
                        .map(
                                event ->
                                        EventMapper.mapToShortDto(
                                                event,
                                                commentariesCount.getOrDefault(event.getId(), 0L),
                                                userFeign.getUserShortById(event.getInitiatorId())))
                        .toList();
        log.debug("Returning {} public event short DTOs", eventsList.size());
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

        Set<Event> eventsSet = events.stream().collect(Collectors.toSet());

        Map<Long, Long> commentariesCount = getCommentariesCount(eventsSet);

        List<EventFullDto> result = events.stream()
                .map(event -> EventMapper.mapToFullDto(
                        event,
                        commentariesCount.getOrDefault(event.getId(), 0L),
                        userFeign.getUserShortById(event.getInitiatorId())))
                .toList();

        log.debug("Returning {} admin event full DTOs", result.size());
        return result;
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

        Set<Event> eventsSet = events.stream().collect(Collectors.toSet());

        Map<Long, Long> commentariesCount = getCommentariesCount(eventsSet);

        List<EventShortDto> result = events.stream()
                .map(event -> EventMapper.mapToShortDto(
                        event,
                        commentariesCount.getOrDefault(event.getId(), 0L),
                        userShortDto))
                .toList();

        log.debug("Returning {} private event short DTOs for userId={}", result.size(), getRequest.userId());
        return result;
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
                saved, 0L, userFeign.getUserShortById(event.getInitiatorId()));
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

        Map<Long, Long> commentariesCount = getCommentariesCount(Set.of(event));

        return EventMapper.mapToFullDto(event, commentariesCount.get(eventId), user);
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
                saved, null, userFeign.getUserShortById(event.getInitiatorId()));
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

        return EventMapper.mapToFullDto(saved, null, user);
    }

    @Override
    @Transactional
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests) {
        log.debug("Updating confirmed requests count for event {}: {}", eventId, confirmedRequests);
        Event event = getEventByIdOrThrow(eventId);
        event.setConfirmedRequests(confirmedRequests);
        eventRepository.save(event);
    }

    @Override
    public List<EventShortDto> getRecommendations(long userId, int maxResult) {
        log.info("Fetching recommendations for userId={}, maxResults={}", userId, maxResult);
        List<Long> recommendationsForUser =
                recommendationsGrpcClient
                        .getRecommendationsForUser(userId, maxResult)
                        .toList()
                        .stream()
                        .map(RecommendedEventProto::getEventId)
                        .toList();
        log.debug("Received {} recommended event IDs from gRPC service", recommendationsForUser.size());

        if (recommendationsForUser.isEmpty()) {
            log.debug("No recommendations found for userId={}", userId);
            return List.of();
        }
        List<Event> events = eventRepository.findAllById(recommendationsForUser);
        log.debug("Loaded {} events from database for recommendations", events.size());
        List<EventShortDto> result = events.stream()
                .map(event -> EventMapper.mapToShortDto(
                        event,
                        null,
                        userFeign.getUserShortById(event.getInitiatorId())))
                .toList();

        log.info("Returning {} recommended events for userId={}", result.size(), userId);
        return result;
    }

    @Override
    public void addLike(Long eventId, long userId) {
        log.info("User {} adding like to event {}", userId, eventId);
        UserShortDto user = userFeign.getUserShortById(userId);
        Event event = getEventByIdOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            log.warn("Cannot like event {} with state {}", eventId, event.getState());
            throw new ValidationException("Event is not published");
        }
        if (!participationRequestFeign.existsByRequesterIdAndEventId(userId, eventId)) {
            log.warn("User {} has not attended event {}, cannot add like", userId, eventId);
            throw new ValidationException("The event was not attended by the user");
        }
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
        log.debug("Like action sent for user {} and event {}", userId, eventId);
    }

    private void sendAction(Long userId, Long eventId, ActionTypeProto actionTypeProto) {
        try {
            log.trace("Preparing to send user action: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionTypeProto);

            UserActionProto userActionProto =
                    UserActionProto.newBuilder()
                            .setUserId(userId)
                            .setEventId(eventId)
                            .setActionType(actionTypeProto)
                            .setTimestamp(
                                    Timestamp.newBuilder()
                                            .setSeconds(Instant.now().getEpochSecond())
                                            .setNanos(Instant.now().getNano())
                                            .build())
                            .build();
            collectorGrpcClient.sendUserAction(userActionProto);
            log.debug("User action sent successfully: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionTypeProto);
        } catch (Exception e) {
            log.error("Failed to send user action: userId={}, eventId={}, actionType={}, error={}",
                    userId, eventId, actionTypeProto, e.getMessage(), e);
        }
    }

    private Map<Long, Long> getCommentariesCount(Set<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        log.debug("Fetching comment counts for {} events", eventIds.size());

        Map<Long, Long> result = commentFeign.countCommentsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventCommentCount::eventId, EventCommentCount::count));

        log.debug("Retrieved comment counts for {} events", result.size());
        return result;
    }

    private Double getRatingForEvent(Long eventId) {
        try {
            log.trace("Fetching rating for event {}", eventId);
            Stream<RecommendedEventProto> stream =
                    recommendationsGrpcClient.getInteractionsCount(eventId);
            Double rating = stream.findFirst().map(RecommendedEventProto::getScore).orElse(0.0);
            log.debug("Retrieved rating for event {}: {}", eventId, rating);
            return rating;
        } catch (Exception e) {
            log.error("Failed to get rating for event {}: {}", eventId, e.getMessage(), e);
            return 0.0;
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
