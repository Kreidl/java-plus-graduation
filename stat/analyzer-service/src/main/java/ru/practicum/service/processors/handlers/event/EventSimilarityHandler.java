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
        log.debug("Processing EventSimilarityAvro: eventA={}, eventB={}, score={}",
                eventSimilarityAvro.getEventA(),
                eventSimilarityAvro.getEventB(),
                eventSimilarityAvro.getScore());

        Long eventIdA = Math.min(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());
        Long eventIdB = Math.max(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());

        Optional<EventSimilarity> eventSimilarityOpt =
                eventSimilarityRepository.findByEventAAndEventB(eventIdA, eventIdB);

        if (eventSimilarityOpt.isEmpty()) {
            log.info("Creating new event similarity record: eventA={}, eventB={}, score={}",
                    eventIdA, eventIdB, eventSimilarityAvro.getScore());
            createEventSimilarity(eventIdA, eventIdB, eventSimilarityAvro);
        } else {
            log.debug("Updating existing event similarity record: eventA={}, eventB={}, oldScore={}, newScore={}",
                    eventIdA, eventIdB,
                    eventSimilarityOpt.get().getScore(),
                    eventSimilarityAvro.getScore());
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

        EventSimilarity saved = eventSimilarityRepository.save(eventSimilarity);
        log.info("Event similarity created successfully: id={}, eventA={}, eventB={}, score={}",
                saved.getId(), eventIdA, eventIdB, eventSimilarityAvro.getScore());
    }

    private void updateEventSimilarity(
            EventSimilarity eventSimilarity, EventSimilarityAvro eventSimilarityAvro) {

        eventSimilarity.setScore(eventSimilarityAvro.getScore());
        eventSimilarity.setTimestamp(eventSimilarityAvro.getTimestamp());

        EventSimilarity updated = eventSimilarityRepository.save(eventSimilarity);
        log.debug("Event similarity updated successfully: id={}, eventA={}, eventB={}, newScore={}",
                updated.getId(),
                eventSimilarity.getEventA(),
                eventSimilarity.getEventB(),
                eventSimilarityAvro.getScore());
    }
}