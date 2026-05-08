package ru.practicum.service;

import java.util.Collection;

import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.dto.UsersGetRequest;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Collection<UserDto> getUsersPaged(UsersGetRequest request) {
        log.debug(
                "Fetching users: ids={}, from={}, size={}",
                request.ids(),
                request.from(),
                request.size());
        Pageable pageable = PageRequest.of(request.from(), request.size());

        Collection<UserDto> result;
        if (request.hasIds()) {
            log.debug("Fetching users by IDs: {}", request.ids());
            result =
                    userRepository.findAllByIdIn(request.ids(), pageable).stream()
                            .map(UserMapper::mapToUserDto)
                            .toList();
        } else {
            result =
                    userRepository.findAll(pageable).stream()
                            .map(UserMapper::mapToUserDto)
                            .toList();
        }

        log.debug("Returned {} users", result.size());
        return result;
    }

    @Override
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info(
                "Creating new user with email='{}', name='{}'",
                newUserRequest.email(),
                newUserRequest.name());
        User user = UserMapper.mapToEntity(newUserRequest);
        User savedUser = userRepository.save(user);
        log.info("User created successfully with id={}", savedUser.getId());
        return UserMapper.mapToUserDto(savedUser);
    }

    @Override
    public void deleteUserById(Long userId) {
        log.info("Deleting user with id={}", userId);
        getUserShortByIdOrThrow(userId);
        userRepository.deleteById(userId);
        log.debug("User with id={} deleted successfully", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserShortDto getUserShortByIdOrThrow(Long userId) {
        log.debug("Fetching user short info for id={}", userId);
        return UserMapper.mapToUserShortDto(
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                NotFoundException.supplier("User with id=%d not found", userId)));
    }
}
