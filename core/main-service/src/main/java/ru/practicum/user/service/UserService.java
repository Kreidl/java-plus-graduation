package ru.practicum.user.service;

import java.util.Collection;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

public interface UserService {
    Collection<UserDto> getUsersPaged(UsersGetRequest request);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUserById(Long userId);
}
