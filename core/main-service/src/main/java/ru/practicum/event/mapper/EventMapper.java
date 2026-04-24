package ru.practicum.event.mapper;

import java.time.LocalDateTime;

import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventMapper {

    public Event mapToEntity(
            NewEventDto newEventDto, Category category, User initiator, Location location) {
        return new Event(
                null,
                newEventDto.annotation(),
                category,
                LocalDateTime.now(),
                newEventDto.description(),
                newEventDto.eventDate(),
                initiator,
                location,
                newEventDto.paid(),
                newEventDto.participantLimit(),
                null,
                newEventDto.requestModeration(),
                EventState.PENDING,
                newEventDto.title());
    }

    public EventFullDto mapToFullDto(
            Event event, long confirmedRequests, Long views, Long commentaries) {
        return new EventFullDto(
                event.getAnnotation(),
                CategoryMapper.mapToDto(event.getCategory()),
                confirmedRequests,
                event.getCreatedOn(),
                event.getDescription(),
                event.getEventDate(),
                event.getId(),
                UserMapper.mapToUserShortDto(event.getInitiator()),
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
            Event event, long confirmedRequests, Long views, Long commentaries) {
        return new EventShortDto(
                event.getAnnotation(),
                CategoryMapper.mapToDto(event.getCategory()),
                confirmedRequests,
                event.getEventDate(),
                event.getId(),
                UserMapper.mapToUserShortDto(event.getInitiator()),
                event.getPaid(),
                event.getTitle(),
                views,
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
