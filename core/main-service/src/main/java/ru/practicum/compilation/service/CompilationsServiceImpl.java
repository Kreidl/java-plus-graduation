package ru.practicum.compilation.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationsMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationsRepository;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.repository.ParticipationRequestRepository;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationsServiceImpl implements CompilationsService {
    private final CompilationsRepository compRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;

    @Override
    public Collection<CompilationDto> findAll(CompilationsPublicGetRequest getRequest) {

        Page<Compilation> page =
                compRepository.findAllByPinned(getRequest.pinned(), getRequest.getPageable());

        Set<Event> events =
                page.stream().flatMap(c -> c.getEvents().stream()).collect(Collectors.toSet());

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);

        return page.stream().map(c -> toDto(c, confirmedRequests)).toList();
    }

    @Override
    public CompilationDto findById(long compId) {

        Compilation compilation =
                compRepository
                        .findWithEventsById(compId)
                        .orElseThrow(
                                NotFoundException.supplier(
                                        "Compilation with id=%d was not found", compId));

        Set<Event> events = compilation.getEvents();

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);

        return toDto(compilation, confirmedRequests);
    }

    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto newCompilationDto) {

        if (compRepository.existsByTitle(newCompilationDto.title())) {
            throw new ConflictException(
                    "Compilation with title=" + newCompilationDto.title() + " already exists");
        }

        Set<Event> events = getEvents(newCompilationDto.events());

        Compilation compilation = CompilationsMapper.mapToEntity(newCompilationDto, events);

        Compilation saved = compRepository.save(compilation);

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);

        return toDto(saved, confirmedRequests);
    }

    @Override
    @Transactional
    public void deleteById(long compId) {
        if (!compRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }

        compRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest updateRequest) {

        Compilation compilation =
                compRepository
                        .findWithEventsById(compId)
                        .orElseThrow(
                                NotFoundException.supplier(
                                        "Compilation with id=%d was not found", compId));

        Set<Event> events = null;
        if (updateRequest.hasEvents()) {
            events = getEvents(updateRequest.events());
        }

        CompilationsMapper.updateEntity(compilation, updateRequest, events);

        Compilation updated = compRepository.save(compilation);

        Set<Event> actualEvents = updated.getEvents();

        Map<Long, Long> confirmedRequests = getConfirmedRequests(actualEvents);

        return toDto(updated, confirmedRequests);
    }

    private CompilationDto toDto(Compilation compilation, Map<Long, Long> confirmedRequests) {
        List<EventShortDto> events =
                compilation.getEvents().stream()
                        .map(
                                event ->
                                        EventMapper.mapToShortDto(
                                                event,
                                                confirmedRequests.getOrDefault(event.getId(), 0L),
                                                null,
                                                null))
                        .toList();

        return CompilationsMapper.mapToDto(compilation, events);
    }

    private Set<Event> getEvents(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Set.of();
        }

        Set<Event> events = eventRepository.findAllByIdIn(eventIds);

        if (events.size() != eventIds.size()) {
            throw new NotFoundException("One or more events were not found");
        }

        return events;
    }

    private Map<Long, Long> getConfirmedRequests(Set<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        return requestRepository.countConfirmedByEventIds(eventIds);
    }
}
