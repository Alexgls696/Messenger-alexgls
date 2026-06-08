package com.alexgls.springboot.notificationsservice.config;

import com.alexgls.springboot.notificationsservice.client.AuthServiceClient;
import com.alexgls.springboot.notificationsservice.client.AuthServiceClientImpl;
import com.alexgls.springboot.notificationsservice.client.MessageStorageServiceClient;
import com.alexgls.springboot.notificationsservice.client.MessageStorageServiceClientImpl;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientsConfig {

    public static String serviceAccessToken;

    @Bean
    public MessageStorageServiceClient messageStorageServiceClient(@Value("${services.message-storage}") String messageStorageServiceUrl) {
        return new MessageStorageServiceClientImpl(RestClient
                .builder()
                .baseUrl(messageStorageServiceUrl)
                .build());
    }

    @Bean
    public AuthServiceClient authServiceClient(@Value("${services.auth}") String authServiceUrl) {
        return new AuthServiceClientImpl(RestClient
                .builder()
                .baseUrl(authServiceUrl)
                .build());
    }

}
