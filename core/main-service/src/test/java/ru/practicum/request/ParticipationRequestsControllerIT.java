package ru.practicum.request;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.request.model.EventRequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ParticipationRequestsIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired EventRepository eventRepository;
    @Autowired ParticipationRequestRepository requestRepository;

    private Long initiatorId;
    private Long requesterId;
    private Long publishedEventId;

    @BeforeEach
    void setUp() {
        User initiator =
                userRepository.save(
                        User.builder().name("initiator").email("initiator@mail.com").build());
        User requester =
                userRepository.save(
                        User.builder().name("requester").email("requester@mail.com").build());

        Category category =
                categoryRepository.save(
                        Category.builder().name("cat-" + System.nanoTime()).build());

        Event published =
                eventRepository.save(
                        Event.builder()
                                .annotation("annotation-annotation-annotation")
                                .category(category)
                                .createdOn(LocalDateTime.now())
                                .description("description-description-description")
                                .eventDate(LocalDateTime.now().plusDays(3))
                                .initiator(initiator)
                                .location(
                                        Location.builder()
                                                .lat(new BigDecimal("55.750000"))
                                                .lon(new BigDecimal("37.610000"))
                                                .build())
                                .paid(false)
                                .participantLimit(10)
                                .requestModeration(true)
                                .state(EventState.PUBLISHED)
                                .title("title")
                                .publishedOn(LocalDateTime.now())
                                .build());

        initiatorId = initiator.getId();
        requesterId = requester.getId();
        publishedEventId = published.getId();
    }

    @Test
    void createRequest_ok_pending() throws Exception {
        mvc.perform(
                        post("/users/{userId}/requests", requesterId)
                                .queryParam("eventId", String.valueOf(publishedEventId))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.event", is(publishedEventId.intValue())))
                .andExpect(jsonPath("$.requester", is(requesterId.intValue())))
                .andExpect(jsonPath("$.created", notNullValue()))
                .andExpect(jsonPath("$.status", is(EventRequestStatus.PENDING.name())));
    }

    @Test
    void createRequest_withoutEventId_400() throws Exception {
        mvc.perform(
                        post("/users/{userId}/requests", requesterId)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_duplicate_409() throws Exception {
        createRequestAndGetId(requesterId, publishedEventId);

        mvc.perform(
                        post("/users/{userId}/requests", requesterId)
                                .queryParam("eventId", String.valueOf(publishedEventId))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void createRequest_byInitiator_409() throws Exception {
        mvc.perform(
                        post("/users/{userId}/requests", initiatorId)
                                .queryParam("eventId", String.valueOf(publishedEventId))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void createRequest_onNotPublished_409() throws Exception {
        User initiator = userRepository.findById(initiatorId).orElseThrow();
        Category category = categoryRepository.findAll().stream().findFirst().orElseThrow();

        Event pending =
                eventRepository.save(
                        Event.builder()
                                .annotation("annotation-annotation-annotation")
                                .category(category)
                                .createdOn(LocalDateTime.now())
                                .description("description-description-description")
                                .eventDate(LocalDateTime.now().plusDays(3))
                                .initiator(initiator)
                                .location(
                                        Location.builder()
                                                .lat(new BigDecimal("55.750000"))
                                                .lon(new BigDecimal("37.610000"))
                                                .build())
                                .paid(false)
                                .participantLimit(10)
                                .requestModeration(true)
                                .state(EventState.PENDING)
                                .title("title")
                                .build());

        mvc.perform(
                        post("/users/{userId}/requests", requesterId)
                                .queryParam("eventId", String.valueOf(pending.getId()))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void createRequest_limitReached_409() throws Exception {
        User initiator = userRepository.findById(initiatorId).orElseThrow();
        Category category = categoryRepository.findAll().stream().findFirst().orElseThrow();

        Event event =
                eventRepository.save(
                        Event.builder()
                                .annotation("annotation-annotation-annotation")
                                .category(category)
                                .createdOn(LocalDateTime.now())
                                .description("description-description-description")
                                .eventDate(LocalDateTime.now().plusDays(3))
                                .initiator(initiator)
                                .location(
                                        Location.builder()
                                                .lat(new BigDecimal("55.750000"))
                                                .lon(new BigDecimal("37.610000"))
                                                .build())
                                .paid(false)
                                .participantLimit(1)
                                .requestModeration(true)
                                .state(EventState.PUBLISHED)
                                .title("title")
                                .publishedOn(LocalDateTime.now())
                                .build());

        long r1 = createRequestAndGetId(requesterId, event.getId());
        String body =
                om.writeValueAsString(Map.of("requestIds", List.of(r1), "status", "CONFIRMED"));
        mvc.perform(
                        patch(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        event.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        User requester2 =
                userRepository.save(User.builder().name("r2").email("r2@mail.com").build());

        mvc.perform(
                        post("/users/{userId}/requests", requester2.getId())
                                .queryParam("eventId", String.valueOf(event.getId()))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void createRequest_moderationFalse_autoConfirmed() throws Exception {
        User initiator = userRepository.findById(initiatorId).orElseThrow();
        Category category = categoryRepository.findAll().stream().findFirst().orElseThrow();

        Event event =
                eventRepository.save(
                        Event.builder()
                                .annotation("annotation-annotation-annotation")
                                .category(category)
                                .createdOn(LocalDateTime.now())
                                .description("description-description-description")
                                .eventDate(LocalDateTime.now().plusDays(3))
                                .initiator(initiator)
                                .location(
                                        Location.builder()
                                                .lat(new BigDecimal("55.750000"))
                                                .lon(new BigDecimal("37.610000"))
                                                .build())
                                .paid(false)
                                .participantLimit(10)
                                .requestModeration(false)
                                .state(EventState.PUBLISHED)
                                .title("title")
                                .publishedOn(LocalDateTime.now())
                                .build());

        mvc.perform(
                        post("/users/{userId}/requests", requesterId)
                                .queryParam("eventId", String.valueOf(event.getId()))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(EventRequestStatus.CONFIRMED.name())));
    }

    @Test
    void getUserRequests_ok_containsCreated() throws Exception {
        long reqId = createRequestAndGetId(requesterId, publishedEventId);

        mvc.perform(get("/users/{userId}/requests", requesterId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].id", hasItem((int) reqId)));
    }

    @Test
    void cancelRequest_ok_canceled() throws Exception {
        long reqId = createRequestAndGetId(requesterId, publishedEventId);

        mvc.perform(
                        patch("/users/{userId}/requests/{requestId}/cancel", requesterId, reqId)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) reqId)))
                .andExpect(jsonPath("$.status", is(EventRequestStatus.CANCELED.name())));
    }

    @Test
    void getEventRequestsByInitiator_ok_containsCreated() throws Exception {
        long reqId = createRequestAndGetId(requesterId, publishedEventId);

        mvc.perform(
                        get(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        publishedEventId)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) reqId)));
    }

    @Test
    void updateRequestsStatus_confirm_ok() throws Exception {
        long reqId = createRequestAndGetId(requesterId, publishedEventId);

        String body =
                om.writeValueAsString(Map.of("requestIds", List.of(reqId), "status", "CONFIRMED"));

        mvc.perform(
                        patch(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        publishedEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests[0].id", is((int) reqId)))
                .andExpect(
                        jsonPath(
                                "$.confirmedRequests[0].status",
                                is(EventRequestStatus.CONFIRMED.name())));
    }

    @Test
    void updateRequestsStatus_reject_ok() throws Exception {
        long reqId = createRequestAndGetId(requesterId, publishedEventId);

        String body =
                om.writeValueAsString(Map.of("requestIds", List.of(reqId), "status", "REJECTED"));

        mvc.perform(
                        patch(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        publishedEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectedRequests[0].id", is((int) reqId)))
                .andExpect(
                        jsonPath(
                                "$.rejectedRequests[0].status",
                                is(EventRequestStatus.REJECTED.name())));
    }

    @Test
    void updateRequestsStatus_nonPending_409() throws Exception {
        long reqId = createRequestAndGetId(requesterId, publishedEventId);

        String confirm =
                om.writeValueAsString(Map.of("requestIds", List.of(reqId), "status", "CONFIRMED"));

        mvc.perform(
                        patch(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        publishedEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(confirm)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        String reject =
                om.writeValueAsString(Map.of("requestIds", List.of(reqId), "status", "REJECTED"));

        mvc.perform(
                        patch(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        publishedEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reject)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void updateRequestsStatus_emptyIds_400() throws Exception {
        String body = om.writeValueAsString(Map.of("requestIds", List.of(), "status", "CONFIRMED"));

        mvc.perform(
                        patch(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        publishedEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRequestsStatus_nullStatus_400() throws Exception {
        String body =
                "{\"requestIds\":["
                        + createRequestAndGetId(requesterId, publishedEventId)
                        + "],\"status\":null}";

        mvc.perform(
                        patch(
                                        "/users/{userId}/events/{eventId}/requests",
                                        initiatorId,
                                        publishedEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private long createRequestAndGetId(long userId, long eventId) throws Exception {
        String json =
                mvc.perform(
                                post("/users/{userId}/requests", userId)
                                        .queryParam("eventId", String.valueOf(eventId))
                                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode node = om.readTree(json);
        return node.get("id").asLong();
    }
}
