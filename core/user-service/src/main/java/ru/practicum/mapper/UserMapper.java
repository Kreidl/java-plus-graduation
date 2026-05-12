package ru.practicum.mapper;

import ru.practicum.model.User;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMapper {
    public User mapToEntity(NewUserRequest dto) {
        return new User(null, dto.name(), dto.email());
    }

    public UserDto mapToUserDto(User user) {
        return new UserDto(user.getEmail(), user.getId(), user.getName());
    }

    public UserShortDto mapToUserShortDto(User user) {
        return new UserShortDto(user.getId(), user.getName());
    }
}
