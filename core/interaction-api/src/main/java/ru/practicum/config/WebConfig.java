package ru.practicum.config;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final DateTimeProperties properties;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(
                new Formatter<LocalDateTime>() {

                    @Override
                    public String print(LocalDateTime object, Locale locale) {
                        return object.format(
                                DateTimeFormatter.ofPattern(properties.getDateTimeFormat()));
                    }

                    @Override
                    public LocalDateTime parse(String text, Locale locale) throws ParseException {
                        return LocalDateTime.parse(
                                text, DateTimeFormatter.ofPattern(properties.getDateTimeFormat()));
                    }
                });
    }
}
