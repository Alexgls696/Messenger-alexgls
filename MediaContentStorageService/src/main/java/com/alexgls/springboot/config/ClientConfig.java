package com.alexgls.springboot.config;

import com.alexgls.springboot.client.InDatabaseStorageServiceRestClient;
import com.alexgls.springboot.client.InDatabaseStorageServiceRestClientImpl;
import com.alexgls.springboot.client.YandexDriveStorageRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class ClientConfig {

    @Value("${yandex.oauth-token}")
    private String oauthToken;

    @Value("${webclient.timeout:10}")
    private int timeoutSeconds;

    @Bean
    public YandexDriveStorageRestClient yandexDriveStorageRestClient() {
        return new YandexDriveStorageRestClient(WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                        .compress(true))) //поддержка сжатия gzip
                .defaultHeader("Authorization", oauthToken)
                .build());
    }

    @Bean
    public InDatabaseStorageServiceRestClient inDatabaseStorageServiceRestClient(@Value("${services.in-database-image-service}") String serviceUrl) {
        return new InDatabaseStorageServiceRestClientImpl(WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                        .compress(true)))
                .baseUrl(serviceUrl)
                .build());
    }

    @Bean
    public WebClient webClient() {
        return WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .followRedirect(true)
                                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                                .compress(true)
                ))
                .build();
    }

}
