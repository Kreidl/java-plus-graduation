package ru.practicum.mapper;

import java.util.List;

import ru.practicum.model.ParticipationRequest;
import ru.practicum.request.dto.ParticipationRequestDto;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ParticipationRequestMapper {

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }
        return new ParticipationRequestDto(
                request.getCreated(),
                request.getEventId(),
                request.getId(),
                request.getRequesterId(),
                request.getStatus().name());
    }

    public List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(ParticipationRequestMapper::toDto).toList();
    }
}
