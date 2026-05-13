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
        kafkaProducer = kafkaProducerConfig.createKafkaHubProducer();
    }

    @Override
    public void handle(UserActionProto userActionProto) {
        log.info(
                "Start of converting UserActionProto with ActionType {} to Avro",
                userActionProto.getActionType().name());
        UserActionAvro userActionAvro = protoToAvro(userActionProto);
        log.debug(
                "End of converting UserActionProto with ActionType {} to Avro {}",
                userActionProto.getActionType().name(),
                userActionAvro);
        ProducerRecord<Long, UserActionAvro> record =
                new ProducerRecord<>(
                        kafkaProducerConfig.getUserActionTopic(),
                        null,
                        userActionAvro.getTimestamp().toEpochMilli(),
                        userActionAvro.getEventId(),
                        userActionAvro);
        log.trace("Save user action {} in topic {}", userActionAvro, record.topic());
        kafkaProducer.send(
                record,
                (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Kafka send failed", exception);
                    } else {
                        log.info(
                                "Message sent to topic {} partition {} offset {}",
                                metadata.topic(),
                                metadata.partition(),
                                metadata.offset());
                    }
                });
        kafkaProducer.flush();
    }

    @Override
    public UserActionAvro protoToAvro(UserActionProto userActionProto) {
        Instant timestamp =
                Instant.ofEpochSecond(
                        userActionProto.getTimestamp().getSeconds(),
                        userActionProto.getTimestamp().getNanos());
        return UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setActionType(getActionTypeAvroFromProto(userActionProto.getActionType()))
                .setTimestamp(timestamp)
                .build();
    }

    private ActionTypeAvro getActionTypeAvroFromProto(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> null;
        };
    }
}
