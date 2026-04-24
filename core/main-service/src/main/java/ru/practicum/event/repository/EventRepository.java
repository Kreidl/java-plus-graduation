package ru.practicum.event.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.service.EventsAdminGetRequest;
import ru.practicum.event.service.EventsPublicGetRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

public interface EventRepository
        extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    static Predicate createPredicate(EventsAdminGetRequest request) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        if (request.hasUsers()) {
            builder.and(event.initiator.id.in(request.users()));
        }

        if (request.hasStates()) {
            builder.and(event.state.in(request.states()));
        }

        if (request.hasCategories()) {
            builder.and(event.category.id.in(request.categories()));
        }

        if (request.hasRangeStart()) {
            builder.and(event.eventDate.goe(request.rangeStart()));
        }

        if (request.hasRangeEnd()) {
            builder.and(event.eventDate.loe(request.rangeEnd()));
        }

        return builder;
    }

    static Predicate createPredicate(EventsPublicGetRequest request) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(event.state.eq(EventState.PUBLISHED));

        if (request.hasPaid()) {
            builder.and(event.paid.eq(request.paid()));
        }

        if (request.hasText()) {
            String text = request.text();
            builder.and(
                    event.annotation
                            .containsIgnoreCase(text)
                            .or(event.description.containsIgnoreCase(text)));
        }

        if (request.hasCategories()) {
            builder.and(event.category.id.in(request.categories()));
        }

        if (request.hasRangeStart()) {
            builder.and(event.eventDate.goe(request.rangeStart()));
        }

        if (request.hasRangeEnd()) {
            builder.and(event.eventDate.loe(request.rangeEnd()));
        }

        if (!(request.hasRangeStart() && request.hasRangeEnd())) {
            builder.and(event.eventDate.goe(LocalDateTime.now()));
        }

        return builder;
    }

    Optional<Event> findByIdAndState(Long id, EventState state);

    Page<Event> findByInitiator_Id(Long initiatorId, Pageable pageable);

    Set<Event> findAllByIdIn(Collection<Long> ids);

    boolean existsById(Long eventId);
}
