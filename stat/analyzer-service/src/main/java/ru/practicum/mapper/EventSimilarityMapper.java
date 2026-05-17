package ru.practicum.mapper;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.event.EventSimilarity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventSimilarityMapper {
    public static EventSimilarity mapToEntity(EventSimilarityAvro eventSimilarityAvro) {
        return new EventSimilarity(
                null,
                eventSimilarityAvro.getEventA(),
                eventSimilarityAvro.getEventB(),
                eventSimilarityAvro.getScore(),
                eventSimilarityAvro.getTimestamp());
    }
}
