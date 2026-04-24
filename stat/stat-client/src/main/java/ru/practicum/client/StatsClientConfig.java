package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class StatsClientConfig {

    @Bean
    public RestTemplate statsRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public StatsClient statsClient(
            RestTemplate statsRestTemplate,
            @Value("${stats-server.id:stats-server}") String serviceId,
            @Value("${spring.application.name:evm-service}") String appName,
            DiscoveryClient discoveryClient) {
        return new StatsClientImpl(statsRestTemplate, serviceId, appName, discoveryClient);
    }
}
