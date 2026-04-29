package ru.practicum.request;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.EventRequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.request.service.ParticipationRequestService;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ParticipationRequestServiceIT {

    @Autowired private ParticipationRequestService requestService;

    @Autowired private ParticipationRequestRepository requestRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private EventRepository eventRepository;

    private User initiator;
    private User requester;
    private Category category;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        initiator = userRepository.save(User.builder().name("init").email("init@mail.com").build());
        requester = userRepository.save(User.builder().name("req").email("req@mail.com").build());
        category = categoryRepository.save(Category.builder().name("cat").build());
    }

    @Test
    void createRequest_shouldConfirmImmediately_whenModerationOff() {
        Event event = eventRepository.save(buildEvent(EventState.PUBLISHED, 10, false));

        ParticipationRequestDto dto =
                requestService.createRequest(requester.getId(), event.getId());

        assertEquals(event.getId(), dto.event());
        assertEquals(requester.getId(), dto.requester());
        assertEquals(EventRequestStatus.CONFIRMED.name(), dto.status());
        assertNotNull(dto.created());
        assertTrue(requestRepository.existsById(dto.id()));
    }

    @Test
    void createRequest_shouldThrowConflict_whenEventNotPublished() {
        Event event = eventRepository.save(buildEvent(EventState.PENDING, 10, true));

        assertThrows(
                ConflictException.class,
                () -> requestService.createRequest(requester.getId(), event.getId()));
    }

    @Test
    void createRequest_shouldThrowConflict_whenRequesterIsInitiator() {
        Event event = eventRepository.save(buildEvent(EventState.PUBLISHED, 10, true));

        assertThrows(
                ConflictException.class,
                () -> requestService.createRequest(initiator.getId(), event.getId()));
    }

    @Test
    void cancelRequest_shouldSetCanceledStatus() {
        Event event = eventRepository.save(buildEvent(EventState.PUBLISHED, 10, true));

        ParticipationRequestDto created =
                requestService.createRequest(requester.getId(), event.getId());
        ParticipationRequestDto canceled =
                requestService.cancelRequest(requester.getId(), created.id());

        assertEquals(EventRequestStatus.CANCELED.name(), canceled.status());
    }

    private Event buildEvent(EventState state, int participantLimit, boolean moderation) {
        return Event.builder()
                .title("title")
                .annotation("ann")
                .description("desc")
                .createdOn(LocalDateTime.now().minusMinutes(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .paid(false)
                .participantLimit(participantLimit)
                .requestModeration(moderation)
                .state(state)
                .initiator(initiator)
                .category(category)
                .location(
                        Location.builder()
                                .lat(BigDecimal.valueOf(55.755800))
                                .lon(BigDecimal.valueOf(37.617300))
                                .build())
                .build();
    }
}
