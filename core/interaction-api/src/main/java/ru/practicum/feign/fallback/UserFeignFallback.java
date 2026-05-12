package ru.practicum.feign.fallback;

import ru.practicum.feign.UserFeign;
import ru.practicum.user.dto.UserShortDto;

import org.springframework.stereotype.Component;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserFeignFallback implements UserFeign {
    @Override
    public UserShortDto getUserShortById(Long userId) throws FeignException {
        fallback();
        return null;
    }

    private void fallback() {
        log.error("Fallback: user service is not responding");
        throw new RuntimeException("User service is not responding, please try again later");
    }
}
