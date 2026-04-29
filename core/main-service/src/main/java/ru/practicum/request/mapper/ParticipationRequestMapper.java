package ru.practicum.request.mapper;

import java.util.List;

import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ParticipationRequestMapper {

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }
        return new ParticipationRequestDto(
                request.getCreated(),
                request.getEvent().getId(),
                request.getId(),
                request.getRequester().getId(),
                request.getStatus().name());
    }

    public List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(ParticipationRequestMapper::toDto).toList();
    }
}
