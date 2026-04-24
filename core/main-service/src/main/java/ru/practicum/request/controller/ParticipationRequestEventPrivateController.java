package ru.practicum.request.controller;

import java.util.List;

import jakarta.validation.Valid;

import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.ParticipationRequestService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events/{eventId}/requests")
public class ParticipationRequestEventPrivateController {

    private final ParticipationRequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId, @PathVariable Long eventId) {
        log.info("Get requests for eventId={} by initiator userId={}", eventId, userId);
        return requestService.getEventRequestsByInitiator(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult updateRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info(
                "Update requests status for eventId={} by userId={}, requestIds={}, status={}",
                eventId,
                userId,
                updateRequest.requestIds(),
                updateRequest.status());
        return requestService.updateEventRequestsStatus(userId, eventId, updateRequest);
    }
}
