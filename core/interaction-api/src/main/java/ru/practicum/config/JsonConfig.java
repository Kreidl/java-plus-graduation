package ru.practicum.config;

import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JsonConfig {
    private final DateTimeProperties properties;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.simpleDateFormat(properties.getDateTimeFormat());
            builder.serializers(
                    new LocalDateTimeSerializer(
                            DateTimeFormatter.ofPattern(properties.getDateTimeFormat())));
            builder.deserializers(
                    new LocalDateTimeDeserializer(
                            DateTimeFormatter.ofPattern(properties.getDateTimeFormat())));
        };
    }
}
