package ru.practicum.config;

import java.util.Map;
import java.util.Properties;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@ToString
@Configuration
@ConfigurationProperties("analyzer.kafka")
public class KafkaConsumerConfig {
    private Map<String, String> topics;
    private Map<String, String> userActionConsumerProperties;
    private Map<String, String> eventSimilarityConsumerProperties;

    public Properties getUserActionConsumerProperties() {
        Properties props = new Properties();
        props.putAll(userActionConsumerProperties);
        return props;
    }

    public Properties getEventSimilarityConsumerProperties() {
        Properties props = new Properties();
        props.putAll(eventSimilarityConsumerProperties);
        return props;
    }

    public String getUserActionTopic() {
        return topics != null ? topics.get("user-actions") : "stats.user-actions.v1";
    }

    public String getEventSimilarityTopic() {
        return topics != null ? topics.get("events-similarity") : "stats.events-similarity.v1";
    }

    public KafkaConsumer<Long, UserActionAvro> createKafkaUserActionConsumer() {
        return new KafkaConsumer<>(getUserActionConsumerProperties());
    }

    public KafkaConsumer<Long, EventSimilarityAvro> createKafkaEventSimilarityConsumer() {
        return new KafkaConsumer<>(getEventSimilarityConsumerProperties());
    }
}
