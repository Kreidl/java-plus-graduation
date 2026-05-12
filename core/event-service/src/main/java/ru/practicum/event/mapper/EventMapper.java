package ru.practicum.event.mapper;

import java.time.LocalDateTime;

import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.*;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.user.dto.UserShortDto;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventMapper {

    public Event mapToEntity(
            NewEventDto newEventDto, Category category, Long initiatorId, Location location) {
        return new Event(
                null,
                newEventDto.annotation(),
                category,
                LocalDateTime.now(),
                newEventDto.description(),
                newEventDto.eventDate(),
                initiatorId,
                location,
                newEventDto.paid(),
                newEventDto.participantLimit(),
                null,
                newEventDto.requestModeration(),
                EventState.PENDING,
                newEventDto.title(),
                0L);
    }

    public EventFullDto mapToFullDto(
            Event event, Long views, Long commentaries, UserShortDto userShortDto) {
        return new EventFullDto(
                event.getAnnotation(),
                CategoryMapper.mapToDto(event.getCategory()),
                event.getConfirmedRequests(),
                event.getCreatedOn(),
                event.getDescription(),
                event.getEventDate(),
                event.getId(),
                userShortDto,
                LocationMapper.mapToDto(event.getLocation()),
                event.getPaid(),
                event.getParticipantLimit(),
                event.getPublishedOn(),
                event.getRequestModeration(),
                event.getState(),
                event.getTitle(),
                views,
                commentaries);
    }

    public EventShortDto mapToShortDto(
            Event event, Long views, Long commentaries, UserShortDto userShortDto) {
        return new EventShortDto(
                event.getAnnotation(),
                CategoryMapper.mapToDto(event.getCategory()),
                event.getConfirmedRequests(),
                event.getEventDate(),
                event.getId(),
                userShortDto,
                event.getPaid(),
                event.getTitle(),
                views,
                commentaries);
    }

    public EventCheckDto mapToCheckDto(Event event, Long commentaries, UserShortDto userShortDto) {
        return new EventCheckDto(
                event.getAnnotation(),
                CategoryMapper.mapToDto(event.getCategory()),
                event.getCreatedOn(),
                event.getDescription(),
                event.getEventDate(),
                event.getId(),
                userShortDto,
                LocationMapper.mapToDto(event.getLocation()),
                event.getPaid(),
                event.getParticipantLimit(),
                event.getPublishedOn(),
                event.getRequestModeration(),
                event.getState(),
                event.getTitle(),
                commentaries);
    }

    public void updateEventFromDto(
            Event event, UpdateEventAdminRequest updateDto, Category newCategory) {
        if (updateDto.hasStateAction()) {
            switch (updateDto.stateAction()) {
                case PUBLISH_EVENT -> {
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> event.setState(EventState.CANCELED);
            }
        }

        updateCommonFields(event, updateDto, newCategory);
    }

    public void updateEventFromDto(
            Event event, UpdateEventUserRequest updateDto, Category newCategory) {
        if (updateDto.hasStateAction()) {
            switch (updateDto.stateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        updateCommonFields(event, updateDto, newCategory);
    }

    private void updateCommonFields(Event event, UpdatableEvent updateDto, Category newCategory) {
        if (updateDto.hasAnnotation()) {
            event.setAnnotation(updateDto.annotation());
        }

        if (updateDto.hasEventDate()) {
            event.setEventDate(updateDto.eventDate());
        }

        if (updateDto.hasCategory()) {
            event.setCategory(newCategory);
        }

        if (updateDto.hasLocation()) {
            Location location = LocationMapper.mapToEntity(updateDto.location());
            event.setLocation(location);
        }

        if (updateDto.hasParticipantLimit()) {
            event.setParticipantLimit(updateDto.participantLimit());
        }

        if (updateDto.hasPaid()) {
            event.setPaid(updateDto.paid());
        }

        if (updateDto.hasRequestModeration()) {
            event.setRequestModeration(updateDto.requestModeration());
        }

        if (updateDto.hasTitle()) {
            event.setTitle(updateDto.title());
        }

        if (updateDto.hasDescription()) {
            event.setDescription(updateDto.description());
        }
    }
}
