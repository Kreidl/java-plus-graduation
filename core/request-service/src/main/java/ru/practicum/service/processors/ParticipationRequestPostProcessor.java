package ru.practicum.service.processors;

import java.time.Instant;

import ru.practicum.client.collector.CollectorGrpcClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.feign.EventFeign;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.request.enums.EventRequestStatus;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipationRequestPostProcessor {

    private final EventFeign eventFeign;
    private final ParticipationRequestRepository requestRepository;
    private final CollectorGrpcClient collectorGrpcClient;

//    @Async("taskExecutor")
//    public void updateConfirmedRequestsAsync(Long eventId) {
//        try {
//            log.debug("Async: Updating confirmed requests for event {}", eventId);
//
//            Long confirmed =
//                    requestRepository.countByEventIdAndStatus(
//                            eventId, EventRequestStatus.CONFIRMED);
//
//            eventFeign.updateConfirmedRequests(eventId, confirmed);
//
//            log.debug("Async: Sent updated count to event-service for event {}", eventId);
//
//        } catch (Exception e) {
//            log.error(
//                    "Async: Failed to update confirmed requests for event {}: {}",
//                    eventId,
//                    e.getMessage(),
//                    e);
//        }
//    }

    @Async("taskExecutor")
    public void sendActionAsync(Long userId, Long eventId, ActionTypeProto actionTypeProto) {
        try {
            log.trace(
                    "Preparing to send user action: userId={}, eventId={}, actionType={}",
                    userId,
                    eventId,
                    actionTypeProto);

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
            log.debug(
                    "User action sent successfully: userId={}, eventId={}, actionType={}",
                    userId,
                    eventId,
                    actionTypeProto);
        } catch (Exception e) {
            log.error(
                    "Failed to send user action: userId={}, eventId={}, actionType={}, error={}",
                    userId,
                    eventId,
                    actionTypeProto,
                    e.getMessage(),
                    e);
        }
    }
}
