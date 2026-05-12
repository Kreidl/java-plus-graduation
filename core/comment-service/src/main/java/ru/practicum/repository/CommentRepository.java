package ru.practicum.repository;

import java.util.List;

import ru.practicum.comment.dto.EventCommentCount;
import ru.practicum.model.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Comment> findAllByEventId(Long eventId, Pageable pageable);

    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    @Query(
            """
                select new ru.practicum.comment.dto.EventCommentCount(c.eventId, count(c))
                from Comment c
                where c.eventId in :eventIds
                group by c.eventId
            """)
    List<EventCommentCount> countCommentsByEventIds(@Param("eventIds") List<Long> eventIds);
}
