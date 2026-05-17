package ru.practicum.repository;

import java.util.List;
import java.util.Optional;

import ru.practicum.model.event.EventSimilarity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query(
            """
            SELECT es
            FROM EventSimilarity AS es
            WHERE (es.eventA IN :eventsIds
            AND es.eventB NOT IN :eventsIds)
            OR (es.eventA NOT IN :eventsIds
            AND es.eventB IN :eventsIds)
            ORDER BY es.score DESC
            LIMIT :limit
            """)
    List<EventSimilarity> findSimilarEvents(
            @Param("eventsIds") List<Long> eventsIds, @Param("limit") long limit);

    @Query(
            """
            SELECT es
            FROM EventSimilarity AS es
            WHERE (es.eventA = :eventId
            AND es.eventB IN :eventsIds)
            OR (es.eventA IN :eventsIds
            AND es.eventB = :eventId)
            ORDER BY es.score DESC
            """)
    List<EventSimilarity> findSimilarEventsByEventsIds(
            @Param("eventId") Long eventId, @Param("eventsIds") List<Long> eventsId);

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);
}
