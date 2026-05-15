package ru.practicum.service.processors;

import java.time.Duration;
import java.util.List;

import ru.practicum.config.KafkaConsumerConfig;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.processors.handlers.user.UserActionHandler;

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
public class UserActionProcessor {
    private final KafkaConsumerConfig kafkaConsumerConfig;
    private final UserActionHandler userActionHandler;
    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    public void start() {
        KafkaConsumer<Long, UserActionAvro> consumer =
                kafkaConsumerConfig.createKafkaUserActionConsumer();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(kafkaConsumerConfig.getUserActionTopic()));
            while (true) {
                log.info("Start reading records");
                ConsumerRecords<Long, UserActionAvro> records =
                        consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                log.info("Start reading records {}", records);
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    UserActionAvro userActionAvro = record.value();
                    log.info("Start reading record {}", userActionAvro);
                    userActionHandler.handle(userActionAvro);
                    log.info("End reading record value {}", userActionAvro);
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
