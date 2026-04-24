package ru.practicum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.exception.IllegalArgumentException;
import ru.practicum.repository.StatRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatServiceImplTest {

    @Mock private StatRepository statRepository;

    @InjectMocks private StatServiceImpl statService;

    @Test
    @DisplayName("Test createEndpointHit with valid Dto")
    void createEndpointHit_WithValidDto_ShouldSaveAndReturnMessage() {
        EndpointHitDto dto = new EndpointHitDto("app", "/uri", "1.2.3.4", LocalDateTime.now());

        statService.createEndpointHit(dto);

        verify(statRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Test getStat with valid params and unique=true")
    void getStat_WithValidParamsAndUniqueTrue_ShouldCallUniqueRepoMethod() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0);
        List<String> uris = List.of("/uri1", "/uri2");
        ViewStatsDto viewStatsDto = new ViewStatsDto("app", "/uri1", 5L);
        when(statRepository.getUniqueStatsWithUris(start, end, uris))
                .thenReturn(Collections.singletonList(viewStatsDto));

        var result = statService.getStat(start, end, uris, true);

        assertEquals(1, result.size());
        assertEquals(viewStatsDto, result.iterator().next());
        verify(statRepository, times(1)).getUniqueStatsWithUris(start, end, uris);
    }

    @Test
    @DisplayName("Test getStat with valid params and unique=false")
    void getStat_WithValidParamsAndUniqueFalse_ShouldCallNotUniqueRepoMethod() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0);
        List<String> uris = List.of("/uri1");
        ViewStatsDto viewStatsDto = new ViewStatsDto("app", "/uri1", 10L);
        when(statRepository.getNotUniqueStatsWithUris(start, end, uris))
                .thenReturn(Collections.singletonList(viewStatsDto));

        var result = statService.getStat(start, end, uris, false);

        assertEquals(1, result.size());
        assertEquals(viewStatsDto, result.iterator().next());
        verify(statRepository, times(1)).getNotUniqueStatsWithUris(start, end, uris);
    }

    @Test
    @DisplayName("Test getStat with invalid dates - start after end")
    void getStat_WithEndBeforeStart_ShouldThrowIllegalArgumentException() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 2, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 10, 0);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> statService.getStat(start, end, null, false));
        assertEquals("The end date must be before start date.", exception.getMessage());
    }

    @Test
    @DisplayName("Test getStat with invalid dates - start equals end")
    void getStat_WithEndEqualToStart_ShouldThrowIllegalArgumentException() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 10, 0);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> statService.getStat(time, time, null, false));
        assertEquals("The end date must be before start date.", exception.getMessage());
    }

    @Test
    @DisplayName("Test getStat with empty uris and unique=true")
    void getStat_WithEmptyUrisAndUniqueTrue_ShouldCallRepoWithUris() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0);
        List<String> uris = Collections.emptyList(); // пустой список
        ViewStatsDto dto = new ViewStatsDto("app", "/uri1", 5L);
        when(statRepository.getUniqueStatsWithUris(start, end, uris))
                .thenReturn(Collections.singletonList(dto));

        var result = statService.getStat(start, end, uris, true);

        assertEquals(1, result.size());
        assertEquals(dto, result.iterator().next());
        verify(statRepository).getUniqueStatsWithUris(start, end, uris);
    }

    @Test
    @DisplayName("Test getStat with empty uris and unique=false")
    void getStat_WithEmptyUrisAndUniqueFalse_ShouldCallRepoWithUris() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0);
        List<String> uris = Collections.emptyList();
        ViewStatsDto dto = new ViewStatsDto("app", "/uri1", 10L);
        when(statRepository.getNotUniqueStatsWithUris(start, end, uris))
                .thenReturn(Collections.singletonList(dto));

        var result = statService.getStat(start, end, uris, false);

        assertEquals(1, result.size());
        assertEquals(dto, result.iterator().next());
        verify(statRepository).getNotUniqueStatsWithUris(start, end, uris);
    }

    @Test
    @DisplayName("Test getStat with null uris and unique=true")
    void getStat_WithNullUrisAndUniqueTrue_ShouldCallRepoWithoutUris() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0);
        ViewStatsDto dto = new ViewStatsDto("app", "/uri1", 5L);
        when(statRepository.getUniqueStatsWithoutUris(start, end))
                .thenReturn(Collections.singletonList(dto));

        var result = statService.getStat(start, end, null, true);

        assertEquals(1, result.size());
        assertEquals(dto, result.iterator().next());
        verify(statRepository).getUniqueStatsWithoutUris(start, end);
    }

    @Test
    @DisplayName("Test getStat with null uris and unique=false")
    void getStat_WithNullUrisAndUniqueFalse_ShouldCallRepoWithoutUris() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0);
        ViewStatsDto dto = new ViewStatsDto("app", "/uri1", 10L);
        when(statRepository.getNotUniqueStatsWithoutUris(start, end))
                .thenReturn(Collections.singletonList(dto));

        var result = statService.getStat(start, end, null, false);

        assertEquals(1, result.size());
        assertEquals(dto, result.iterator().next());
        verify(statRepository).getNotUniqueStatsWithoutUris(start, end);
    }
}
