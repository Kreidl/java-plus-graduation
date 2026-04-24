package ru.practicum.request;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
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
class ParticipationRequestBatchUpdateIT {

    @Autowired private ParticipationRequestService requestService;

    @Autowired private ParticipationRequestRepository requestRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private EventRepository eventRepository;

    private User initiator;
    private User u1;
    private User u2;
    private User u3;
    private Category category;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        initiator = userRepository.save(User.builder().name("init").email("init@mail.com").build());
        u1 = userRepository.save(User.builder().name("u1").email("u1@mail.com").build());
        u2 = userRepository.save(User.builder().name("u2").email("u2@mail.com").build());
        u3 = userRepository.save(User.builder().name("u3").email("u3@mail.com").build());
        category = categoryRepository.save(Category.builder().name("cat").build());
    }

    @Test
    void batchConfirm_shouldConfirmUpToLimit_andRejectRest_andRejectOtherPendingWhenLimitReached() {
        Event event = eventRepository.save(buildEvent(EventState.PUBLISHED, 2, true));

        ParticipationRequestDto r1 = requestService.createRequest(u1.getId(), event.getId());
        ParticipationRequestDto r2 = requestService.createRequest(u2.getId(), event.getId());
        ParticipationRequestDto r3 = requestService.createRequest(u3.getId(), event.getId());

        EventRequestStatusUpdateResult result =
                requestService.updateEventRequestsStatus(
                        initiator.getId(),
                        event.getId(),
                        new EventRequestStatusUpdateRequest(
                                List.of(r1.id(), r2.id(), r3.id()), EventRequestStatus.CONFIRMED));

        assertEquals(2, result.confirmedRequests().size());
        assertEquals(1, result.rejectedRequests().size());

        List<ParticipationRequestDto> all =
                requestService.getEventRequestsByInitiator(initiator.getId(), event.getId());

        long confirmed =
                all.stream()
                        .filter(r -> r.status().equals(EventRequestStatus.CONFIRMED.name()))
                        .count();
        long rejected =
                all.stream()
                        .filter(r -> r.status().equals(EventRequestStatus.REJECTED.name()))
                        .count();

        assertEquals(2, confirmed);
        assertEquals(1, rejected);
    }

    @Test
    void batchReject_shouldRejectOnlyProvidedPending() {
        Event event = eventRepository.save(buildEvent(EventState.PUBLISHED, 10, true));

        ParticipationRequestDto r1 = requestService.createRequest(u1.getId(), event.getId());
        ParticipationRequestDto r2 = requestService.createRequest(u2.getId(), event.getId());

        EventRequestStatusUpdateResult result =
                requestService.updateEventRequestsStatus(
                        initiator.getId(),
                        event.getId(),
                        new EventRequestStatusUpdateRequest(
                                List.of(r1.id(), r2.id()), EventRequestStatus.REJECTED));

        assertEquals(0, result.confirmedRequests().size());
        assertEquals(2, result.rejectedRequests().size());
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
