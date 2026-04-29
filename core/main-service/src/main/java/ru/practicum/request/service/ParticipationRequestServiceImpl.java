package ru.practicum.request.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ForbiddenAccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.ParticipationRequestMapper;
import ru.practicum.request.model.EventRequestStatus;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = getUserByIdOrThrow(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (requestRepository.existsByEvent_IdAndRequester_Id(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator can't participate in own event");
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Event must be published");
        }

        long confirmed =
                requestRepository.countByEvent_IdAndStatus(eventId, EventRequestStatus.CONFIRMED);
        if (event.getParticipantLimit() != null
                && event.getParticipantLimit() > 0
                && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit has been reached");
        }

        EventRequestStatus status = EventRequestStatus.PENDING;
        if (Boolean.FALSE.equals(event.getRequestModeration())
                || event.getParticipantLimit() == null
                || event.getParticipantLimit() == 0) {
            status = EventRequestStatus.CONFIRMED;
        }

        ParticipationRequest request =
                ParticipationRequest.builder()
                        .event(event)
                        .requester(user)
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
                requestRepository.findAllByRequester_IdOrderByCreatedDesc(userId));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        getUserByIdOrThrow(userId);
        ParticipationRequest request = getRequestByIdOrThrow(requestId);

        if (!request.getRequester().getId().equals(userId)) {
            throw new ForbiddenAccessException("You can't cancel request that is not yours");
        }

        request.setStatus(EventRequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequestsByInitiator(Long userId, Long eventId) {
        getUserByIdOrThrow(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenAccessException(
                    "You can't view requests for event that is not yours");
        }

        return ParticipationRequestMapper.toDtoList(
                requestRepository.findAllByEvent_IdOrderByCreatedAsc(eventId));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        getUserByIdOrThrow(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenAccessException(
                    "You can't update requests for event that is not yours");
        }

        Set<Long> ids = new HashSet<>(updateRequest.requestIds());
        List<ParticipationRequest> requests =
                requestRepository.findAllByIdInAndEvent_Id(ids, eventId);
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

    private EventRequestStatusUpdateResult confirmRequests(
            Event event, List<ParticipationRequest> requests) {
        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        boolean moderation = Boolean.TRUE.equals(event.getRequestModeration());

        if (!moderation || limit == 0) {
            requests.forEach(r -> r.setStatus(EventRequestStatus.CONFIRMED));
            requestRepository.saveAll(requests);
            return new EventRequestStatusUpdateResult(
                    ParticipationRequestMapper.toDtoList(requests), List.of());
        }

        long confirmed =
                requestRepository.countByEvent_IdAndStatus(
                        event.getId(), EventRequestStatus.CONFIRMED);
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
                    requestRepository.findAllByEvent_IdAndStatus(
                            event.getId(), EventRequestStatus.PENDING);

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

    private User getUserByIdOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new NotFoundException("User with id=%d not found".formatted(userId)));
    }

    private Event getEventByIdOrThrow(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        "Event with id=%d not found".formatted(eventId)));
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
