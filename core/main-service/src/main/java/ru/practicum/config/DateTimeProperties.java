package ru.practicum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app")
public class DateTimeProperties {
    @Getter @Setter private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
}
