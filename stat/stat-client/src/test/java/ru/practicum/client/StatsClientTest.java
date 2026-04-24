package ru.practicum.client;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;

import ru.practicum.dto.EndpointHitDto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class StatsClientTest {

    private MockRestServiceServer server;
    private StatsClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client =
                new StatsClientImpl(
                        restTemplate,
                        new DefaultServiceInstance(
                                "stats-server", "stats-server", "localhost", 9090, true),
                        "ewm-main-service");
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void hit_shouldPostToHitEndpoint() {
        EndpointHitDto dto =
                EndpointHitDto.builder()
                        .app("ewm-main-service")
                        .uri("/events")
                        .ip("127.0.0.1")
                        .timestamp(LocalDateTime.of(2025, 12, 19, 10, 0, 0))
                        .build();

        server.expect(requestTo("http://localhost:9090/hit"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Content-Type", containsString("application/json")))
                .andRespond(withSuccess());

        client.hit(dto);
    }

    @Test
    void getStats_shouldBuildQueryWithUrisAndUnique() {
        String responseJson =
                """
                [
                  {"app":"ewm-main-service","uri":"/events/1","hits":5},
                  {"app":"ewm-main-service","uri":"/events/2","hits":2}
                ]
                """;

        server.expect(
                        requestTo(
                                allOf(
                                        startsWith("http://localhost:9090/stats?"),
                                        containsString("start=2025-12-19%2000:00:00"),
                                        containsString("end=2025-12-19%2023:59:59"),
                                        containsString("unique=true"),
                                        containsString("uris=/events/1"),
                                        containsString("uris=/events/2"))))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        var res =
                client.getStats(
                        LocalDateTime.of(2025, 12, 19, 0, 0, 0),
                        LocalDateTime.of(2025, 12, 19, 23, 59, 59),
                        List.of("/events/1", "/events/2"),
                        true);

        Assertions.assertEquals(2, res.size());
        Assertions.assertEquals("/events/1", res.getFirst().uri());
        Assertions.assertEquals(5L, res.getFirst().hits());
    }

    @Test
    void getStats_shouldNotIncludeUrisParamWhenUrisEmpty() {
        String responseJson = "[]";

        server.expect(
                        requestTo(
                                allOf(
                                        startsWith("http://localhost:9090/stats?"),
                                        not(containsString("uris=")),
                                        containsString("unique=false"))))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        var res =
                client.getStats(
                        LocalDateTime.of(2025, 12, 19, 0, 0, 0),
                        LocalDateTime.of(2025, 12, 19, 1, 0, 0),
                        List.of(),
                        false);

        Assertions.assertTrue(res.isEmpty());
    }
}
