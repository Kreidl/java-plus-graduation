package ru.practicum.compilation.controller;

import jakarta.validation.Valid;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.service.CompilationsService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/compilations")
public class CompilationsAdminController {
    private final CompilationsService compService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto saveCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        log.info("Admin save compilation requested with body={}", newCompilationDto);
        return compService.save(newCompilationDto);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable long compId) {
        log.info("Admin delete compilation requested with id={}", compId);
        compService.deleteById(compId);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(
            @PathVariable long compId, @RequestBody @Valid UpdateCompilationRequest updateRequest) {
        log.info("Admin update compilation requested with id={}, body={}", compId, updateRequest);
        return compService.update(compId, updateRequest);
    }
}
