package ru.practicum.controller;

import ru.practicum.feign.UserFeign;
import ru.practicum.service.UserService;
import ru.practicum.user.dto.UserShortDto;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserControllerFeignClient implements UserFeign {
    private final UserService userService;

    @Override
    public UserShortDto getUserShortById(Long userId) {
        return userService.getUserShortByIdOrThrow(userId);
    }
}
