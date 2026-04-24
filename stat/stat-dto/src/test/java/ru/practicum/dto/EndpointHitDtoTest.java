package ru.practicum.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class EndpointHitDtoTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Serialization test (object → JSON)")
    void serialize_ShouldConvertToObjectToJson() throws JsonProcessingException {
        LocalDateTime now = LocalDateTime.of(2025, 6, 15, 10, 30, 45);
        EndpointHitDto dto =
                EndpointHitDto.builder()
                        .app("ewm-main-service")
                        .uri("/events/1")
                        .ip("192.168.1.1")
                        .timestamp(now)
                        .build();

        String json = objectMapper.writeValueAsString(dto);

        assertNotNull(json);
        assertTrue(json.contains("\"app\":\"ewm-main-service\""));
        assertTrue(json.contains("\"uri\":\"/events/1\""));
        assertTrue(json.contains("\"ip\":\"192.168.1.1\""));
        assertTrue(json.contains("\"timestamp\":\"2025-06-15 10:30:45\""));
    }

    @Test
    @DisplayName("Deserialization test (JSON → object)")
    void deserialize_ShouldConvertFromJsonToObject() throws JsonProcessingException {
        String json =
                "{\"app\": \"stat-service\", \"uri\": \"/ping\", \"ip\": \"127.0.0.1\","
                        + "\"timestamp\": \"2025-12-17 15:45:30\"}";

        EndpointHitDto dto = objectMapper.readValue(json, EndpointHitDto.class);

        assertEquals("stat-service", dto.app());
        assertEquals("/ping", dto.uri());
        assertEquals("127.0.0.1", dto.ip());
        assertEquals(LocalDateTime.of(2025, 12, 17, 15, 45, 30), dto.timestamp());
    }

    @Test
    @DisplayName("Deserialization test with wrong date format")
    void deserialize_WithInvalidDateFormat_ShouldThrowException() {
        String json =
                "{\"app\": \"service\", \"uri\": \"/test\", \"ip\": \"1.2.3.4\","
                        + "\"created\": \"17-12-2025 15:45\"}";

        assertThrows(
                JsonProcessingException.class,
                () -> objectMapper.readValue(json, EndpointHitDto.class));
    }

    @Test
    @DisplayName("Validation test: all fields are filled")
    void validate_WithValidData_ShouldHaveNoViolations() {
        EndpointHitDto dto =
                EndpointHitDto.builder()
                        .app("app")
                        .uri("/uri")
                        .ip("1.2.3.4")
                        .timestamp(LocalDateTime.now())
                        .build();

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Validation test: app empty")
    void validate_WithBlankApp_ShouldHaveViolation() {
        EndpointHitDto dto =
                EndpointHitDto.builder()
                        .app("")
                        .uri("/uri")
                        .ip("1.2.3.4")
                        .timestamp(LocalDateTime.now())
                        .build();

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("app")));
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Service name must not be blank")));
    }

    @Test
    @DisplayName("Validation test: timestamp null")
    void validate_WithNullTimestamp_ShouldHaveViolation() {
        EndpointHitDto dto =
                EndpointHitDto.builder()
                        .app("app")
                        .uri("/uri")
                        .ip("1.2.3.4")
                        .timestamp(null)
                        .build();

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("timestamp")));
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("View date must not be null")));
    }

    @Test
    @DisplayName("Validation test: all fields null/blank")
    void validate_WithAllFieldsInvalid_ShouldHaveMultipleViolations() {
        EndpointHitDto dto = new EndpointHitDto(null, null, null, null);

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertEquals(4, violations.size());
    }
}
