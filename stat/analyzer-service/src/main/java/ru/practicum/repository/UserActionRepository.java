package ru.practicum.repository;

import java.util.List;
import java.util.Optional;

import ru.practicum.model.user.UserAction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    @Query(
            """
            SELECT ua
            FROM UserAction AS ua
            WHERE ua.userId = :userId
            ORDER BY ua.timestamp DESC
            LIMIT :limit
            """)
    List<UserAction> findEventsByUserIdOrderByTimestampDesc(
            @Param("userId") Long userId, @Param("limit") long limit);

    List<Long> findEventsIdByUserId(Long userId);

    List<UserAction> findUserActionByEventIdIn(List<Long> eventsId);

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);
}
