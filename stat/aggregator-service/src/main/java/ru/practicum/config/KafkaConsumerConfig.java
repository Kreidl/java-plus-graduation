package ru.practicum.config;

import java.util.Map;
import java.util.Properties;

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
@ConfigurationProperties("aggregator.kafka")
public class KafkaConsumerConfig {
    private Map<String, String> topics;
    private Map<String, String> userActionConsumerProperties;

    public Properties getUserActionConsumerProperties() {
        Properties props = new Properties();
        props.putAll(userActionConsumerProperties);
        return props;
    }

    public String getUserActionTopic() {
        return topics != null ? topics.get("user-actions") : "stats.user-actions.v1";
    }

    public KafkaConsumer<Long, UserActionAvro> createKafkaUserActionConsumer() {
        return new KafkaConsumer<>(getUserActionConsumerProperties());
    }
}
