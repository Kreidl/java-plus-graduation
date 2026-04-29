package ru.practicum.compilation.repository;

import java.util.Optional;

import ru.practicum.compilation.model.Compilation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompilationsRepository extends JpaRepository<Compilation, Long> {

    Page<Compilation> findAllByPinned(boolean pinned, Pageable pageable);

    @EntityGraph(attributePaths = "events")
    Optional<Compilation> findWithEventsById(Long id);

    boolean existsByTitle(String title);
}
