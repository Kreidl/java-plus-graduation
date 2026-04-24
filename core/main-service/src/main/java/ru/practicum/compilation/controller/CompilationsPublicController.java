package ru.practicum.compilation.controller;

import java.util.Collection;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.service.CompilationsPublicGetRequest;
import ru.practicum.compilation.service.CompilationsService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/compilations")
public class CompilationsPublicController {
    private final CompilationsService compService;

    @GetMapping
    public Collection<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        CompilationsPublicGetRequest getRequest =
                new CompilationsPublicGetRequest(pinned, from, size);
        log.info("Public get compilations requested with params= {}", getRequest);
        return compService.findAll(getRequest);
    }

    @GetMapping("/{compId}")
    public CompilationDto getById(@PathVariable long compId) {
        log.info("Public get compilation by id requested with id={}", compId);
        return compService.findById(compId);
    }
}
