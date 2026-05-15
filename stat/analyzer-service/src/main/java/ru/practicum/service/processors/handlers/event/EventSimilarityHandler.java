package ru.practicum.service.processors.handlers.event;

import java.util.Optional;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.event.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityHandler {
    private final EventSimilarityRepository eventSimilarityRepository;

    public void handle(EventSimilarityAvro eventSimilarityAvro) {
        Long eventIdA = Math.min(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());
        Long eventIdB = Math.max(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());
        Optional<EventSimilarity> eventSimilarityOpt =
                eventSimilarityRepository.findByEventAAndEventB(eventIdA, eventIdB);
        if (eventSimilarityOpt.isEmpty()) {
            createEventSimilarity(eventIdA, eventIdB, eventSimilarityAvro);
        } else {
            updateEventSimilarity(eventSimilarityOpt.get(), eventSimilarityAvro);
        }
    }

    private void createEventSimilarity(
            Long eventIdA, Long eventIdB, EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = new EventSimilarity();
        eventSimilarity.setEventA(eventIdA);
        eventSimilarity.setEventB(eventIdB);
        eventSimilarity.setScore(eventSimilarityAvro.getScore());
        eventSimilarity.setTimestamp(eventSimilarityAvro.getTimestamp());
        eventSimilarityRepository.save(eventSimilarity);
    }

    private void updateEventSimilarity(
            EventSimilarity eventSimilarity, EventSimilarityAvro eventSimilarityAvro) {
        eventSimilarity.setScore(eventSimilarityAvro.getScore());
        eventSimilarity.setTimestamp(eventSimilarityAvro.getTimestamp());
        eventSimilarityRepository.save(eventSimilarity);
    }
}
