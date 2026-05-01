package ru.practicum.service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.*;

import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.enums.EventState;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventFeign eventFeign;
    private final UserFeign userFeign;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        UserShortDto user = getUserByIdOrThrow(userId);
        EventFullDto event = getEventByIdOrThrow(eventId);

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }

        if (event.initiator().id().equals(userId)) {
            throw new ConflictException("Initiator can't participate in own event");
        }

        if (!EventState.PUBLISHED.equals(event.state())) {
            throw new ConflictException("Event must be published");
        }

        long confirmed =
                requestRepository.countByEventIdAndStatus(eventId, EventRequestStatus.CONFIRMED);
        if (event.participantLimit() != null
                && event.participantLimit() > 0
                && confirmed >= event.participantLimit()) {
            throw new ConflictException("Participant limit has been reached");
        }

        EventRequestStatus status = EventRequestStatus.PENDING;
        if (Boolean.FALSE.equals(event.requestModeration())
                || event.participantLimit() == null
                || event.participantLimit() == 0) {
            status = EventRequestStatus.CONFIRMED;
        }

        ParticipationRequest request =
                ParticipationRequest.builder()
                        .eventId(event.id())
                        .requesterId(user.id())
                        .created(LocalDateTime.now())
                        .status(status)
                        .build();

        ParticipationRequest saved = requestRepository.save(request);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        getUserByIdOrThrow(userId);
        return ParticipationRequestMapper.toDtoList(
                requestRepository.findAllByRequesterIdOrderByCreatedDesc(userId));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        getUserByIdOrThrow(userId);
        ParticipationRequest request = getRequestByIdOrThrow(requestId);

        if (!request.getRequesterId().equals(userId)) {
            throw new ForbiddenAccessException("You can't cancel request that is not yours");
        }

        request.setStatus(EventRequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequestsByInitiator(Long userId, Long eventId) {
        getUserByIdOrThrow(userId);
        EventFullDto event = getEventByIdOrThrow(eventId);

        if (!event.initiator().id().equals(userId)) {
            throw new ForbiddenAccessException(
                    "You can't view requests for event that is not yours");
        }

        return ParticipationRequestMapper.toDtoList(
                requestRepository.findAllByEventIdOrderByCreatedAsc(eventId));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        getUserByIdOrThrow(userId);
        EventFullDto event = getEventByIdOrThrow(eventId);

        if (!event.initiator().id().equals(userId)) {
            throw new ForbiddenAccessException(
                    "You can't update requests for event that is not yours");
        }

        Set<Long> ids = new HashSet<>(updateRequest.requestIds());
        List<ParticipationRequest> requests =
                requestRepository.findAllByIdInAndEventId(ids, eventId);
        if (requests.size() != ids.size()) {
            throw new NotFoundException("Some requests were not found");
        }

        for (ParticipationRequest r : requests) {
            if (!EventRequestStatus.PENDING.equals(r.getStatus())) {
                throw new ConflictException("Only PENDING requests can be updated");
            }
        }

        EventRequestStatus targetStatus = updateRequest.status();
        if (EventRequestStatus.CONFIRMED.equals(targetStatus)) {
            return confirmRequests(event, requests);
        }
        if (EventRequestStatus.REJECTED.equals(targetStatus)) {
            requests.forEach(r -> r.setStatus(EventRequestStatus.REJECTED));
            requestRepository.saveAll(requests);
            return new EventRequestStatusUpdateResult(
                    List.of(), ParticipationRequestMapper.toDtoList(requests));
        }

        throw new ConflictException("Unsupported status update: " + targetStatus);
    }

    @Override
    public Map<Long, Long> countConfirmedByEventIds(Collection<Long> eventIds) {
        return requestRepository.countConfirmedByEventIds(eventIds);
    }

    private EventRequestStatusUpdateResult confirmRequests(
            EventFullDto event, List<ParticipationRequest> requests) {
        int limit = event.participantLimit() == null ? 0 : event.participantLimit();
        boolean moderation = Boolean.TRUE.equals(event.requestModeration());

        if (!moderation || limit == 0) {
            requests.forEach(r -> r.setStatus(EventRequestStatus.CONFIRMED));
            requestRepository.saveAll(requests);
            return new EventRequestStatusUpdateResult(
                    ParticipationRequestMapper.toDtoList(requests), List.of());
        }

        long confirmed =
                requestRepository.countByEventIdAndStatus(event.id(), EventRequestStatus.CONFIRMED);
        long available = limit - confirmed;
        if (available <= 0) {
            throw new ConflictException("Participant limit has been reached");
        }

        List<ParticipationRequest> confirmedRequests;
        List<ParticipationRequest> rejectedRequests;

        if (requests.size() <= available) {
            requests.forEach(r -> r.setStatus(EventRequestStatus.CONFIRMED));
            requestRepository.saveAll(requests);
            confirmedRequests = requests;
            rejectedRequests = List.of();
        } else {
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
                toReject.forEach(r -> r.setStatus(EventRequestStatus.REJECTED));
                requestRepository.saveAll(toReject);
            }
        }

        return new EventRequestStatusUpdateResult(
                ParticipationRequestMapper.toDtoList(confirmedRequests),
                ParticipationRequestMapper.toDtoList(rejectedRequests));
    }

    private static Set<Long> idsOf(Collection<ParticipationRequest> requests) {
        return requests.stream().map(ParticipationRequest::getId).collect(Collectors.toSet());
    }

    private UserShortDto getUserByIdOrThrow(Long userId) {
        return userFeign.getUserShortById(userId);
    }

    private EventFullDto getEventByIdOrThrow(Long eventId) {
        return eventFeign.getEventFullDtoById(eventId);
    }

    private ParticipationRequest getRequestByIdOrThrow(Long requestId) {
        return requestRepository
                .findById(requestId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        "Request with id=%d not found".formatted(requestId)));
    }
}
