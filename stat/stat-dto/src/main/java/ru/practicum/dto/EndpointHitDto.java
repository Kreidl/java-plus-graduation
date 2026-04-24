package ru.practicum.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

@Builder
public record EndpointHitDto(
        @NotBlank(message = "Service name must not be blank") String app,
        @NotBlank(message = "URI must not be blank") String uri,
        @NotBlank(message = "IP must not be blank") String ip,
        @NotNull(message = "View date must not be null") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                LocalDateTime timestamp) {}
