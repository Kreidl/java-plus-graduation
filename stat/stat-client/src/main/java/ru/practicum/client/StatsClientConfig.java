package ru.practicum.client;

import ru.practicum.client.exception.StatsServerUnavailable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class StatsClientConfig {

    private final DiscoveryClient discoveryClient;

    public StatsClientConfig(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Bean
    public RestTemplate statsRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public StatsClient statsClient(
            RestTemplate statsRestTemplate,
            @Value("${stats-server.id:stats-server}") String serviceId,
            @Value("${spring.application.name:evm-service}") String appName) {
        return new StatsClientImpl(statsRestTemplate, getInstance(serviceId), appName);
    }

    private ServiceInstance getInstance(String serviceId) {
        try {
            return discoveryClient.getInstances(serviceId).getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + serviceId, exception);
        }
    }
}
