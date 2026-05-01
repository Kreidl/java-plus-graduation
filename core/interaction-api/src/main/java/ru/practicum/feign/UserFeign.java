package ru.practicum.feign;

import jakarta.validation.constraints.NotNull;

import ru.practicum.feign.fallback.UserFeignFallback;
import ru.practicum.user.dto.UserShortDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import feign.FeignException;

@Validated
@FeignClient(name = "user-service", path = "/api/v1/user", fallback = UserFeignFallback.class)
public interface UserFeign {
    @GetMapping("/{userId}/short")
    UserShortDto getUserShortById(@NotNull @PathVariable(name = "userId") Long userId)
            throws FeignException;
}
