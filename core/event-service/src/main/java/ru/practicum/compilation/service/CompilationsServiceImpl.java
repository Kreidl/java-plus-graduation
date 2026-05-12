package ru.practicum.compilation.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.CompilationsPublicGetRequest;
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
import ru.practicum.feign.UserFeign;

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
    private final UserFeign userFeign;

    @Override
    public Collection<CompilationDto> findAll(CompilationsPublicGetRequest getRequest) {
        log.info(
                "Fetching compilations: pinned={}, from={}, size={}",
                getRequest.pinned(),
                getRequest.getPageable().getPageNumber(),
                getRequest.getPageable().getPageSize());

        Page<Compilation> page =
                compRepository.findAllByPinned(getRequest.pinned(), getRequest.getPageable());
        log.debug("Found {} compilations", page.getTotalElements());

        Set<Event> events =
                page.stream().flatMap(c -> c.getEvents().stream()).collect(Collectors.toSet());
        log.debug("Loaded {} unique events for compilations", events.size());

        return page.stream().map(this::toDto).toList();
    }

    @Override
    public CompilationDto findById(long compId) {
        log.debug("Fetching compilation by id={}", compId);

        Compilation compilation =
                compRepository
                        .findWithEventsById(compId)
                        .orElseThrow(
                                NotFoundException.supplier(
                                        "Compilation with id=%d was not found", compId));

        log.debug("Found compilation with id={}", compId);
        return toDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto newCompilationDto) {
        log.info("Creating new compilation with title='{}'", newCompilationDto.title());

        if (compRepository.existsByTitle(newCompilationDto.title())) {
            log.warn("Compilation with title='{}' already exists", newCompilationDto.title());
            throw new ConflictException(
                    "Compilation with title=" + newCompilationDto.title() + " already exists");
        }

        Set<Event> events = getEvents(newCompilationDto.events());
        log.debug("Resolved {} events for compilation", events.size());

        Compilation compilation = CompilationsMapper.mapToEntity(newCompilationDto, events);
        Compilation saved = compRepository.save(compilation);

        log.info("Compilation created successfully with id={}", saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteById(long compId) {
        log.info("Deleting compilation with id={}", compId);

        if (!compRepository.existsById(compId)) {
            log.warn("Compilation with id={} not found for deletion", compId);
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }

        compRepository.deleteById(compId);
        log.debug("Compilation with id={} deleted successfully", compId);
    }

    @Override
    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest updateRequest) {
        log.info("Updating compilation with id={}", compId);

        Compilation compilation =
                compRepository
                        .findWithEventsById(compId)
                        .orElseThrow(
                                NotFoundException.supplier(
                                        "Compilation with id=%d was not found", compId));

        Set<Event> events = null;
        if (updateRequest.hasEvents()) {
            log.debug("Updating events list for compilation {}", compId);
            events = getEvents(updateRequest.events());
        }

        CompilationsMapper.updateEntity(compilation, updateRequest, events);
        Compilation updated = compRepository.save(compilation);

        log.info("Compilation with id={} updated successfully", compId);
        return toDto(updated);
    }

    private CompilationDto toDto(Compilation compilation) {
        log.debug(
                "Mapping compilation {} to DTO ({} events)",
                compilation.getId(),
                compilation.getEvents().size());

        List<EventShortDto> events =
                compilation.getEvents().stream()
                        .map(
                                event ->
                                        EventMapper.mapToShortDto(
                                                event,
                                                null,
                                                null,
                                                userFeign.getUserShortById(event.getInitiatorId())))
                        .toList();

        return CompilationsMapper.mapToDto(compilation, events);
    }

    private Set<Event> getEvents(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            log.debug("No event IDs provided, returning empty set");
            return Set.of();
        }

        log.debug("Fetching {} events by IDs", eventIds.size());
        Set<Event> events = eventRepository.findAllByIdIn(eventIds);

        if (events.size() != eventIds.size()) {
            log.warn("Expected {} events, but found {}", eventIds.size(), events.size());
            throw new NotFoundException("One or more events were not found");
        }

        return events;
    }
}
