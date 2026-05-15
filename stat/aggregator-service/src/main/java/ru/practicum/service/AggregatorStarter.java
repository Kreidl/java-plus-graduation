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
        log.info("Starting AggregatorStarter service");

        KafkaConsumer<Long, UserActionAvro> consumer = kafkaConsumerConfig.createKafkaUserActionConsumer();
        KafkaProducer<Long, EventSimilarityAvro> producer = kafkaProducerConfig.createKafkaEventSimilarityProducer();

        log.info("Kafka consumer and producer initialized");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered, waking up consumer");
            consumer.wakeup();
        }));

        try {
            String topic = kafkaConsumerConfig.getUserActionTopic();
            consumer.subscribe(List.of(topic));
            log.info("Subscribed to Kafka topic: {}", topic);

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                if (records.isEmpty()) {
                    log.trace("No records received in this poll cycle");
                    continue;
                }

                log.debug("Received {} records from topic {}", records.count(), topic);

                int processedCount = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    log.trace("Processing record: key={}, offset={}, partition={}",
                            record.key(), record.offset(), record.partition());

                    List<EventSimilarityAvro> eventsSimilarity = aggregatorUpdater.handle(record);

                    if (!eventsSimilarity.isEmpty()) {
                        log.debug("Generated {} similarity records for user action", eventsSimilarity.size());

                        for (EventSimilarityAvro eventSimilarityAvro : eventsSimilarity) {
                            ProducerRecord<Long, EventSimilarityAvro> producerRecord =
                                    new ProducerRecord<>(
                                            kafkaProducerConfig.getEventSimilarityTopic(),
                                            eventSimilarityAvro);

                            producer.send(producerRecord);
                            log.debug("Sent similarity message to topic={} partition={}",
                                    producerRecord.topic(), producerRecord.partition());

                            manageOffsets(record, processedCount, consumer);
                            processedCount++;
                        }
                        consumer.commitAsync();
                        log.trace("Committed offsets asynchronously after processing {} records", processedCount);
                    }
                }
            }
        } catch (WakeupException e) {
            log.info("Consumer woken up, shutting down gracefully");
            // Expected during shutdown - ignore
        } catch (Exception e) {
            log.error("Unexpected error while processing Kafka records", e);
        } finally {
            try {
                log.info("Flushing producer and committing final offsets");
                producer.flush();
                consumer.commitSync(currentOffsets);
            } catch (Exception e) {
                log.warn("Error during final commit/flush, continuing with shutdown", e);
            } finally {
                log.info("Closing Kafka consumer");
                consumer.close();
                log.info("Closing Kafka producer");
                producer.close();
                log.info("AggregatorStarter shutdown completed");
            }
        }
    }

    public static void manageOffsets(
            ConsumerRecord<Long, UserActionAvro> record,
            int count,
            KafkaConsumer<Long, UserActionAvro> consumer) {

        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        OffsetAndMetadata offset = new OffsetAndMetadata(record.offset() + 1);

        currentOffsets.put(partition, offset);
        log.trace("Tracked offset for topic={} partition={} offset={}",
                record.topic(), record.partition(), record.offset() + 1);

        // Commit every 10 records to balance performance and durability
        if (count % 10 == 0) {
            log.debug("Committing offsets for {} partitions", currentOffsets.size());
            consumer.commitAsync(
                    currentOffsets,
                    (offsets, exception) -> {
                        if (exception != null) {
                            log.warn("Failed to commit offsets: {}, error={}", offsets, exception.getMessage(), exception);
                        } else {
                            log.trace("Successfully committed offsets: {}", offsets);
                        }
                    });
        }
    }
}