package ru.practicum.request.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.practicum.request.model.EventRequestStatus;
import ru.practicum.request.model.ParticipationRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    boolean existsByEvent_IdAndRequester_Id(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByRequester_IdOrderByCreatedDesc(Long requesterId);

    List<ParticipationRequest> findAllByEvent_IdOrderByCreatedAsc(Long eventId);

    long countByEvent_IdAndStatus(Long eventId, EventRequestStatus status);

    List<ParticipationRequest> findAllByIdInAndEvent_Id(Collection<Long> ids, Long eventId);

    List<ParticipationRequest> findAllByEvent_IdAndStatus(Long eventId, EventRequestStatus status);

    @Query(
            """
            select pr.event.id as eventId, count(pr.id) as cnt
            from ParticipationRequest pr
            where pr.status = :status
              and pr.event.id in :eventIds
            group by pr.event.id
            """)
    List<ConfirmedRequestsCount> countByEventIdsAndStatusRaw(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("status") EventRequestStatus status);

    default Map<Long, Long> countConfirmedByEventIds(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return countByEventIdsAndStatusRaw(eventIds, EventRequestStatus.CONFIRMED).stream()
                .collect(
                        Collectors.toMap(
                                ConfirmedRequestsCount::getEventId,
                                ConfirmedRequestsCount::getCnt));
    }
}
