package ru.practicum.repository;

import java.util.stream.Collectors;
import java.util.*;

import ru.practicum.model.ParticipationRequest;
import ru.practicum.request.dto.ConfirmedRequestsCount;
import ru.practicum.request.enums.EventRequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByRequesterIdOrderByCreatedDesc(Long requesterId);

    List<ParticipationRequest> findAllByEventIdOrderByCreatedAsc(Long eventId);

    long countByEventIdAndStatus(Long eventId, EventRequestStatus status);

    List<ParticipationRequest> findAllByIdInAndEventId(Collection<Long> ids, Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, EventRequestStatus status);

    @Query(
            """
            select pr.eventId, count(pr.id) as cnt
            from ParticipationRequest pr
            where pr.status = :status
              and pr.eventId in :eventIds
            group by pr.eventId
            """)
    List<ConfirmedRequestsCount> countByEventIdsAndStatusRaw(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("status") EventRequestStatus status);

    default Map<Long, Long> countConfirmedByEventIds(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> validEventIds =
                eventIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());

        if (validEventIds.isEmpty()) {
            return Map.of();
        }

        return countByEventIdsAndStatusRaw(validEventIds, EventRequestStatus.CONFIRMED).stream()
                .filter(r -> r.getEventId() != null) // ✅ Фильтруем null в результате
                .filter(r -> r.getCnt() != null) // ✅ На всякий случай
                .collect(
                        Collectors.toMap(
                                ConfirmedRequestsCount::getEventId,
                                ConfirmedRequestsCount::getCnt,
                                (v1, v2) -> v1 + v2 // ✅ merge function на случай дубликатов ключей
                                ));
    }
}
