package ru.practicum.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.practicum.config.KafkaConsumerConfig;
import ru.practicum.config.KafkaProducerConfig;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorStarter {
    private final KafkaConsumerConfig kafkaConsumerConfig;
    private final KafkaProducerConfig kafkaProducerConfig;
    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private final AggregatorUpdater aggregatorUpdater;
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public void start() {
        KafkaConsumer<Long, UserActionAvro> consumer =
                kafkaConsumerConfig.createKafkaUserActionConsumer();
        KafkaProducer<Long, EventSimilarityAvro> producer =
                kafkaProducerConfig.createKafkaEventSimilarityProducer();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(kafkaConsumerConfig.getUserActionTopic()));
            while (true) {
                ConsumerRecords<Long, UserActionAvro> records =
                        consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    List<EventSimilarityAvro> eventsSimilarity = aggregatorUpdater.handle(record);
                    if (!eventsSimilarity.isEmpty()) {
                        for (EventSimilarityAvro eventSimilarityAvro : eventsSimilarity) {
                            ProducerRecord<Long, EventSimilarityAvro> producerRecord =
                                    new ProducerRecord<>(
                                            kafkaProducerConfig.getEventSimilarityTopic(),
                                            eventSimilarityAvro);
                            producer.send(producerRecord);
                            log.debug(
                                    "Message sent to topic {} partition {}",
                                    producerRecord.topic(),
                                    producerRecord.partition());
                            manageOffsets(record, count, consumer);
                            count++;
                        }
                        consumer.commitAsync();
                    }
                }
            }
        } catch (WakeupException ignored) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Error while processing events from sensors", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Close consumer");
                consumer.close();
                log.info("Close producer");
                producer.close();
            }
        }
    }

    public static void manageOffsets(
            ConsumerRecord<Long, UserActionAvro> record,
            int count,
            KafkaConsumer<Long, UserActionAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1));
        if (count % 10 == 0) {
            consumer.commitAsync(
                    currentOffsets,
                    (offsets, exception) -> {
                        if (exception != null) {
                            log.warn("Error in offset fixation process: {}", offsets, exception);
                        }
                    });
        }
    }
}
