package ru.practicum.comment.repository;

import java.util.List;

import ru.practicum.comment.model.Comment;

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
                select new ru.practicum.comment.repository.EventCommentCount(c.event.id, count(c))
                from Comment c
                where c.event.id in :eventIds
                group by c.event.id
            """)
    List<EventCommentCount> countCommentsByEventIds(@Param("eventIds") List<Long> eventIds);
}
