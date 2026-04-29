package ru.practicum.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ViewStatsDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Serialization test: object → JSON")
    void serialize_ShouldConvertToObjectToJson() throws JsonProcessingException {
        ViewStatsDto dto = new ViewStatsDto("main-service", "/events", 42L);

        String json = objectMapper.writeValueAsString(dto);

        assertNotNull(json);
        assertTrue(json.contains("\"app\":\"main-service\""));
        assertTrue(json.contains("\"uri\":\"/events\""));
        assertTrue(json.contains("\"hits\":42"));
    }

    @Test
    @DisplayName("Deserialization test: JSON → object")
    void deserialize_ShouldConvertFromJsonToObject() throws JsonProcessingException {
        String json = "{\"app\": \"main-service\", \"uri\": \"/events\", \"hits\": 150}";

        ViewStatsDto dto = objectMapper.readValue(json, ViewStatsDto.class);

        assertEquals("main-service", dto.app());
        assertEquals("/events", dto.uri());
        assertEquals(150L, dto.hits());
    }

    @Test
    @DisplayName("Reversibility test")
    void roundTripSerialization_ShouldPreserveData() throws JsonProcessingException {
        ViewStatsDto original = new ViewStatsDto("main-service", "/events", 100L);

        String json = objectMapper.writeValueAsString(original);
        ViewStatsDto deserialized = objectMapper.readValue(json, ViewStatsDto.class);

        assertEquals(original.app(), deserialized.app());
        assertEquals(original.uri(), deserialized.uri());
        assertEquals(original.hits(), deserialized.hits());
    }
}
