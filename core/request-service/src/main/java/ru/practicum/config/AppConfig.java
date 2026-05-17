package ru.practicum.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AppConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Основные настройки пула
        executor.setCorePoolSize(4); // Минимальное число потоков
        executor.setMaxPoolSize(16); // Максимальное число потоков
        executor.setQueueCapacity(100); // Очередь задач при перегрузке
        executor.setThreadNamePrefix("async-"); // Префикс имён потоков (для логов)

        // Политика при переполнении очереди
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Корректное завершение при остановке приложения
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
