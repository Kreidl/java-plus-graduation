package ru.practicum.service.handler;

import java.time.Instant;

import ru.practicum.config.KafkaProducerConfig;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserActionHandlerImpl implements UserActionHandler {
    private final KafkaProducerConfig kafkaProducerConfig;
    private final KafkaProducer<Long, UserActionAvro> kafkaProducer;

    public UserActionHandlerImpl(KafkaProducerConfig kafkaProducerConfig) {
        this.kafkaProducerConfig = kafkaProducerConfig;
        kafkaProducer = kafkaProducerConfig.createKafkaUserActionProducer();
    }

    @Override
    public void handle(UserActionProto userActionProto) {
        log.debug(
                "Handling user action: userId={}, eventId={}, actionType={}, timestamp={}",
                userActionProto.getUserId(),
                userActionProto.getEventId(),
                userActionProto.getActionType(),
                userActionProto.getTimestamp());

        log.trace("Converting UserActionProto to UserActionAvro");
        UserActionAvro userActionAvro = protoToAvro(userActionProto);
        log.trace("Conversion completed: UserActionAvro={}", userActionAvro);

        String topic = kafkaProducerConfig.getUserActionTopic();
        ProducerRecord<Long, UserActionAvro> record =
                new ProducerRecord<>(
                        topic,
                        null, // partition (null = auto-assign)
                        userActionAvro.getTimestamp().toEpochMilli(), // key for ordering
                        userActionAvro.getEventId(), // key for partitioning
                        userActionAvro);

        log.trace(
                "Sending message to Kafka: topic={}, key={}, eventId={}",
                topic,
                record.key(),
                userActionAvro.getEventId());

        kafkaProducer.send(
                record,
                (metadata, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Failed to send user action to Kafka: topic={}, eventId={},"
                                        + " error={}",
                                topic,
                                userActionAvro.getEventId(),
                                exception.getMessage(),
                                exception);
                    } else {
                        log.debug(
                                "User action sent to Kafka successfully: topic={}, partition={},"
                                        + " offset={}, eventId={}",
                                metadata.topic(),
                                metadata.partition(),
                                metadata.offset(),
                                userActionAvro.getEventId());
                    }
                });

        // Flush to ensure message is sent (consider async in production)
        kafkaProducer.flush();
        log.trace(
                "Kafka producer flushed for user action: userId={}, eventId={}",
                userActionProto.getUserId(),
                userActionProto.getEventId());
    }

    @Override
    public UserActionAvro protoToAvro(UserActionProto userActionProto) {
        log.trace(
                "Converting timestamp: seconds={}, nanos={}",
                userActionProto.getTimestamp().getSeconds(),
                userActionProto.getTimestamp().getNanos());

        Instant timestamp =
                Instant.ofEpochSecond(
                        userActionProto.getTimestamp().getSeconds(),
                        userActionProto.getTimestamp().getNanos());

        ActionTypeAvro actionTypeAvro = getActionTypeAvroFromProto(userActionProto.getActionType());
        log.trace(
                "Mapped ActionTypeProto {} to ActionTypeAvro {}",
                userActionProto.getActionType(),
                actionTypeAvro);

        return UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setActionType(actionTypeAvro)
                .setTimestamp(timestamp)
                .build();
    }

    private ActionTypeAvro getActionTypeAvroFromProto(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> {
                log.trace("Mapped ACTION_VIEW proto to VIEW avro");
                yield ActionTypeAvro.VIEW;
            }
            case ACTION_REGISTER -> {
                log.trace("Mapped ACTION_REGISTER proto to REGISTER avro");
                yield ActionTypeAvro.REGISTER;
            }
            case ACTION_LIKE -> {
                log.trace("Mapped ACTION_LIKE proto to LIKE avro");
                yield ActionTypeAvro.LIKE;
            }
            default -> {
                log.warn("Unknown ActionTypeProto value: {}, defaulting to null", actionTypeProto);
                yield null;
            }
        };
    }
}
