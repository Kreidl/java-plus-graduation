package ru.practicum.service;

import java.util.Collection;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.dto.UsersGetRequest;

public interface UserService {
    Collection<UserDto> getUsersPaged(UsersGetRequest request);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUserById(Long userId);

    UserShortDto getUserShortByIdOrThrow(Long userId);
}
