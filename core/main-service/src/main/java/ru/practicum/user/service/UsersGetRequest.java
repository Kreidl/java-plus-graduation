package ru.practicum.user.service;

import java.util.List;

public record UsersGetRequest(int from, int size, List<Long> ids) {
    public boolean hasIds() {
        return ids != null && !ids.isEmpty();
    }
}
