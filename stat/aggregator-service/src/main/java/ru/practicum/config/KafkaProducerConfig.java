package ru.practicum.config;

import java.util.Map;
import java.util.Properties;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@Configuration
@ConfigurationProperties("aggregator.kafka")
public class KafkaProducerConfig {
    private Map<String, String> topics;
    private Map<String, String> eventSimilarityProducerProperties;

    public Properties getEventSimilarityProducerProperties() {
        Properties props = new Properties();
        props.putAll(eventSimilarityProducerProperties);
        return props;
    }

    public String getEventSimilarityTopic() {
        return topics != null ? topics.get("events-similarity") : "stats.events-similarity.v1";
    }

    public KafkaProducer<Long, EventSimilarityAvro> createKafkaEventSimilarityProducer() {
        return new KafkaProducer<>(getEventSimilarityProducerProperties());
    }
}
