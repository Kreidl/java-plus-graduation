package ru.practicum.client;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.client.exception.StatsServerUnavailable;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class StatsClientImpl implements StatsClient {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RetryTemplate retryTemplate;
    private final RestTemplate restTemplate;
    private final String app;
    private final String serviceId;
    private final DiscoveryClient discoveryClient;

    public StatsClientImpl(
            RestTemplate restTemplate,
            String serviceId,
            String app,
            DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        retryTemplate = RetryTemplate.builder().maxAttempts(3).fixedBackoff(3000).build();
        this.restTemplate = restTemplate;
        this.serviceId = serviceId;
        this.app = app;
    }

    @Override
    public void hit(EndpointHitDto dto) {
        String url = makeUri("/hit");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHitDto> request = new HttpEntity<>(dto, headers);
        restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
    }

    @Override
    public void hit(HttpServletRequest request) {
        EndpointHitDto dto =
                EndpointHitDto.builder()
                        .app(app)
                        .uri(request.getRequestURI())
                        .ip(request.getRemoteAddr())
                        .timestamp(LocalDateTime.now())
                        .build();

        hit(dto);
    }

    @Override
    public List<ViewStatsDto> getStats(
            LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        UriComponentsBuilder b =
                UriComponentsBuilder.fromHttpUrl(makeUri("/stats"))
                        .queryParam("start", start.format(FMT))
                        .queryParam("end", end.format(FMT))
                        .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                b.queryParam("uris", uri);
            }
        }

        String url = b.build(false).toUriString();

        ResponseEntity<ViewStatsDto[]> resp = restTemplate.getForEntity(url, ViewStatsDto[].class);
        ViewStatsDto[] body = resp.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    private String makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance(serviceId));
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path)
                .toString();
    }

    private ServiceInstance getInstance(String serviceId) {
        try {
            return discoveryClient.getInstances(serviceId).getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + serviceId, exception);
        }
    }
}
