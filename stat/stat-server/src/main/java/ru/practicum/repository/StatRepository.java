package ru.practicum.repository;

import java.time.LocalDateTime;
import java.util.List;

import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.Stats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StatRepository extends JpaRepository<Stats, Long> {
    @Query(
            """
            SELECT new ru.practicum.dto.ViewStatsDto(st.app, st.uri, COUNT(DISTINCT st.ip))
            FROM Stats AS st
            WHERE st.created BETWEEN :start AND :end AND st.uri IN :uris
            GROUP BY st.uri, st.app
            ORDER BY COUNT(DISTINCT st.ip) DESC
            """)
    List<ViewStatsDto> getUniqueStatsWithUris(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);

    @Query(
            """
            SELECT new ru.practicum.dto.ViewStatsDto(st.app, st.uri, COUNT(st.ip))
            FROM Stats AS st
            WHERE st.created BETWEEN :start AND :end AND st.uri IN :uris
            GROUP BY st.uri, st.app
            ORDER BY COUNT(st.ip) DESC
            """)
    List<ViewStatsDto> getNotUniqueStatsWithUris(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);

    @Query(
            """
            SELECT new ru.practicum.dto.ViewStatsDto(st.app, st.uri, COUNT(DISTINCT st.ip))
            FROM Stats AS st
            WHERE st.created BETWEEN :start AND :end
            GROUP BY st.uri, st.app
            ORDER BY COUNT(DISTINCT st.ip) DESC
            """)
    List<ViewStatsDto> getUniqueStatsWithoutUris(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(
            """
            SELECT new ru.practicum.dto.ViewStatsDto(st.app, st.uri, COUNT(st.ip))
            FROM Stats AS st
            WHERE st.created BETWEEN :start AND :end
            GROUP BY st.uri, st.app
            ORDER BY COUNT(st.ip) DESC
            """)
    List<ViewStatsDto> getNotUniqueStatsWithoutUris(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
