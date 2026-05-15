package ru.practicum.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.*;

import ru.practicum.client.collector.CollectorGrpcClient;
import ru.practicum.event.dto.EventCheckDto;
import ru.practicum.event.enums.EventState;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ForbiddenAccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventFeign;
import ru.practicum.feign.UserFeign;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.enums.EventRequestStatus;
import ru.practicum.user.dto.UserShortDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.protobuf.Timestamp;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventFeign eventFeign;
    private final UserFeign userFeign;
    private final CollectorGrpcClient collectorGrpcClient;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Creating participation request: userId={}, eventId={}", userId, eventId);

        UserShortDto user = userFeign.getUserShortById(userId);
        EventCheckDto event = getEventOrThrowConflict(eventId);

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            log.warn("Duplicate request: userId={} already requested eventId={}", userId, eventId);
            throw new ConflictException("Request already exists");
        }

        if (event.initiator().id().equals(userId)) {
            log.warn("User {} attempted to join their own event {}", userId, eventId);
            throw new ConflictException("Initiator can't participate in own event");
        }

        if (!EventState.PUBLISHED.equals(event.state())) {
            log.warn(
                    "Event {} is not published (state={}), request rejected",
                    eventId,
                    event.state());
            throw new ConflictException("Event must be published");
        }

        long confirmed =
                requestRepository.countByEventIdAndStatus(eventId, EventRequestStatus.CONFIRMED);
        if (event.participantLimit() != null
                && event.participantLimit() > 0
                && confirmed >= event.participantLimit()) {
            log.warn("Event {} reached participant limit ({})", eventId, event.participantLimit());
            throw new ConflictException("Participant limit has been reached");
        }

        EventRequestStatus status = EventRequestStatus.PENDING;
        if (Boolean.FALSE.equals(event.requestModeration())
                || event.participantLimit() == null
                || event.participantLimit() == 0) {
            status = EventRequestStatus.CONFIRMED;
            log.debug("Request auto-confirmed due to no moderation or unlimited participants");
        }

        ParticipationRequest request =
                ParticipationRequest.builder()
                        .eventId(event.id())
                        .requesterId(user.id())
                        .created(LocalDateTime.now())
                        .status(status)
                        .build();

        ParticipationRequest saved = requestRepository.save(request);
        log.info(
                "Participation request created with id={}, status={}",
                saved.getId(),
                saved.getStatus());

        if (EventRequestStatus.CONFIRMED.equals(saved.getStatus())) {
            updateConfirmedRequests(event.id());
        }
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.debug("Fetching requests for userId={}", userId);
        userFeign.getUserShortById(userId);
        List<ParticipationRequestDto> result =
                ParticipationRequestMapper.toDtoList(
                        requestRepository.findAllByRequesterIdOrderByCreatedDesc(userId));
        log.debug("Found {} requests for userId={}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("User {} cancelling request {}", userId, requestId);

        userFeign.getUserShortById(userId);
        ParticipationRequest request = getRequestByIdOrThrow(requestId);

        if (!request.getRequesterId().equals(userId)) {
            log.warn(
                    "User {} attempted to cancel request {} owned by user {}",
                    userId,
                    requestId,
                    request.getRequesterId());
            throw new ForbiddenAccessException("You can't cancel request that is not yours");
        }

        request.setStatus(EventRequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Request {} cancelled successfully", requestId);

        updateConfirmedRequests(request.getEventId());
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequestsByInitiator(Long userId, Long eventId) {
        log.info("Fetching requests for event {} by initiator {}", eventId, userId);

        userFeign.getUserShortById(userId);
        EventCheckDto event = getEventOrThrowConflict(eventId);

        if (!event.initiator().id().equals(userId)) {
            log.warn(
                    "User {} attempted to view requests for event {} owned by user {}",
                    userId,
                    eventId,
                    event.initiator().id());
            throw new ForbiddenAccessException(
                    "You can't view requests for event that is not yours");
        }

        List<ParticipationRequestDto> result =
                ParticipationRequestMapper.toDtoList(
                        requestRepository.findAllByEventIdOrderByCreatedAsc(eventId));
        log.debug("Found {} requests for event {}", result.size(), eventId);
        return result;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        log.info(
                "Updating request statuses for event {} by user {}: requestIds={}, newStatus={}",
                eventId,
                userId,
                updateRequest.requestIds(),
                updateRequest.status());

        userFeign.getUserShortById(userId);
        EventCheckDto event = getEventOrThrowConflict(eventId);

        if (!event.initiator().id().equals(userId)) {
            log.warn(
                    "User {} attempted to update requests for event {} owned by user {}",
                    userId,
                    eventId,
                    event.initiator().id());
            throw new ForbiddenAccessException(
                    "You can't update requests for event that is not yours");
        }

        Set<Long> ids = new HashSet<>(updateRequest.requestIds());
        List<ParticipationRequest> requests =
                requestRepository.findAllByIdInAndEventId(ids, eventId);

        if (requests.size() != ids.size()) {
            log.warn(
                    "Expected {} requests, but found {} for event {}",
                    ids.size(),
                    requests.size(),
                    eventId);
            throw new NotFoundException("Some requests were not found");
        }

        for (ParticipationRequest r : requests) {
            if (!EventRequestStatus.PENDING.equals(r.getStatus())) {
                log.warn(
                        "Request {} has status {}, only PENDING can be updated",
                        r.getId(),
                        r.getStatus());
                throw new ConflictException("Only PENDING requests can be updated");
            }
        }

        EventRequestStatus targetStatus = updateRequest.status();
        if (EventRequestStatus.CONFIRMED.equals(targetStatus)) {
            log.debug("Confirming {} requests for event {}", requests.size(), eventId);
            return confirmRequests(event, requests);
        }
        if (EventRequestStatus.REJECTED.equals(targetStatus)) {
            log.debug("Rejecting {} requests for event {}", requests.size(), eventId);
            requests.forEach(r -> r.setStatus(EventRequestStatus.REJECTED));
            requestRepository.saveAll(requests);
            updateConfirmedRequests(eventId);
            return new EventRequestStatusUpdateResult(
                    List.of(), ParticipationRequestMapper.toDtoList(requests));
        }

        log.warn("Unsupported status update: {}", targetStatus);
        throw new ConflictException("Unsupported status update: " + targetStatus);
    }

    @Override
    public Map<Long, Long> countConfirmedByEventIds(Collection<Long> eventIds) {
        log.debug("Counting confirmed requests for {} events", eventIds.size());
        return requestRepository.countConfirmedByEventIds(eventIds);
    }

    @Override
    public Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId) {
        return requestRepository.existsByEventIdAndRequesterId(eventId, requesterId);
    }

    private EventRequestStatusUpdateResult confirmRequests(
            EventCheckDto event, List<ParticipationRequest> requests) {
        int limit = event.participantLimit() == null ? 0 : event.participantLimit();
        boolean moderation = Boolean.TRUE.equals(event.requestModeration());

        if (!moderation || limit == 0) {
            log.debug(
                    "Auto-confirming all {} requests (no moderation or unlimited)",
                    requests.size());
            requests.forEach(r -> r.setStatus(EventRequestStatus.CONFIRMED));
            requestRepository.saveAll(requests);
            return new EventRequestStatusUpdateResult(
                    ParticipationRequestMapper.toDtoList(requests), List.of());
        }

        long confirmed =
                requestRepository.countByEventIdAndStatus(event.id(), EventRequestStatus.CONFIRMED);
        long available = limit - confirmed;

        if (available <= 0) {
            log.warn(
                    "Event {} reached participant limit, cannot confirm more requests", event.id());
            throw new ConflictException("Participant limit has been reached");
        }

        List<ParticipationRequest> confirmedRequests;
        List<ParticipationRequest> rejectedRequests;

        if (requests.size() <= available) {
            log.debug("Confirming all {} requests (within limit)", requests.size());
            requests.forEach(r -> r.setStatus(EventRequestStatus.CONFIRMED));
            requestRepository.saveAll(requests);
            confirmedRequests = requests;
            rejectedRequests = List.of();
        } else {
            log.debug(
                    "Confirming first {} requests, rejecting remaining {}",
                    available,
                    requests.size() - available);
            confirmedRequests = requests.subList(0, (int) available);
            rejectedRequests = requests.subList((int) available, requests.size());
            confirmedRequests.forEach(r -> r.setStatus(EventRequestStatus.CONFIRMED));
            rejectedRequests.forEach(r -> r.setStatus(EventRequestStatus.REJECTED));
            requestRepository.saveAll(requests);
        }

        long nowConfirmed = confirmed + confirmedRequests.size();
        if (nowConfirmed >= limit) {
            List<ParticipationRequest> pendingToReject =
                    requestRepository.findAllByEventIdAndStatus(
                            event.id(), EventRequestStatus.PENDING);

            Set<Long> touched = idsOf(requests);
            List<ParticipationRequest> toReject =
                    pendingToReject.stream()
                            .filter(r -> !touched.contains(r.getId()))
                            .collect(Collectors.toList());

            if (!toReject.isEmpty()) {
                log.debug("Rejecting {} pending requests due to limit reached", toReject.size());
                toReject.forEach(r -> r.setStatus(EventRequestStatus.REJECTED));
                requestRepository.saveAll(toReject);
            }
        }

        updateConfirmedRequests(event.id());
        return new EventRequestStatusUpdateResult(
                ParticipationRequestMapper.toDtoList(confirmedRequests),
                ParticipationRequestMapper.toDtoList(rejectedRequests));
    }

    private static Set<Long> idsOf(Collection<ParticipationRequest> requests) {
        return requests.stream().map(ParticipationRequest::getId).collect(Collectors.toSet());
    }

    private ParticipationRequest getRequestByIdOrThrow(Long requestId) {
        log.debug("Looking up request with id={}", requestId);
        return requestRepository
                .findById(requestId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        "Request with id=%d not found".formatted(requestId)));
    }

    @Transactional
    public void updateConfirmedRequests(Long eventId) {
        log.debug("Updating confirmed requests count for event {}", eventId);
        Long confirmedRequests =
                requestRepository.countByEventIdAndStatus(eventId, EventRequestStatus.CONFIRMED);
        confirmedRequests = (confirmedRequests == null) ? 0 : confirmedRequests;
        eventFeign.updateConfirmedRequests(eventId, confirmedRequests);
        log.debug(
                "Sent updated count {} to event-service for event {}", confirmedRequests, eventId);
    }

    private EventCheckDto getEventOrThrowConflict(Long eventId) {
        log.debug("Fetching event check data for eventId={} via Feign", eventId);
        try {
            EventCheckDto event = eventFeign.getEventCheckDtoById(eventId);
            log.debug("Successfully fetched event data for eventId={}", eventId);
            return event;
        } catch (FeignException.NotFound e) {
            log.debug("Event {} not found via Feign, converting to ConflictException", eventId);
            throw new ConflictException("Event with id=" + eventId + " not found or not published");
        } catch (FeignException e) {
            log.error("Failed to fetch event {} via Feign: {}", eventId, e.getMessage(), e);
            throw new ConflictException("Failed to validate event with id=" + eventId);
        }
    }

    private void sendAction(Long userId, Long eventId, ActionTypeProto actionTypeProto) {
        try {
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
        } catch (Exception e) {
            log.error("Error sending user action");
        }
    }
}
