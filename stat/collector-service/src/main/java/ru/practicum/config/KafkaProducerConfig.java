package ru.practicum.config;

import java.util.Map;
import java.util.Properties;

import ru.practicum.ewm.stats.avro.UserActionAvro;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@ToString
@Configuration
@ConfigurationProperties("collector.kafka")
public class KafkaProducerConfig {
    private Map<String, String> topics;
    private Map<String, String> userActionProducerProperties;

    public Properties getUserActionProducerProperties() {
        Properties props = new Properties();
        props.putAll(userActionProducerProperties);
        return props;
    }

    public String getUserActionTopic() {
        return topics != null ? topics.get("user-actions") : "stats.user-actions.v1";
    }

    public KafkaProducer<Long, UserActionAvro> createKafkaUserActionProducer() {
        return new KafkaProducer<>(getUserActionProducerProperties());
    }
}
