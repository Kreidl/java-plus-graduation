package ru.practicum.service.processors;

import java.time.Duration;
import java.util.List;

import ru.practicum.config.KafkaConsumerConfig;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.service.processors.handlers.event.EventSimilarityHandler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor implements Runnable {
    private final KafkaConsumerConfig kafkaConsumerConfig;
    private final EventSimilarityHandler eventSimilarityHandler;
    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    @Override
    public void run() {
        KafkaConsumer<Long, EventSimilarityAvro> consumer =
                kafkaConsumerConfig.createKafkaEventSimilarityConsumer();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(kafkaConsumerConfig.getEventSimilarityTopic()));
            while (true) {
                log.info("Start reading records");
                ConsumerRecords<Long, EventSimilarityAvro> records =
                        consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                log.info("Start reading records {}", records);
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    EventSimilarityAvro eventSimilarityAvro = record.value();
                    log.info("Start reading record {}", eventSimilarityAvro);
                    eventSimilarityHandler.handle(eventSimilarityAvro);
                    log.info("End reading record value {}", eventSimilarityAvro);
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Error of reading data");
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
                log.info("Consumer closed");
            }
        }
    }
}
