package com.alexgls.springboot.userdetailsservice.config;

import com.alexgls.springboot.userdetailsservice.client.AuthServiceClient;
import com.alexgls.springboot.userdetailsservice.client.AuthServiceClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientsConfig {
    @Bean
    public AuthServiceClient authWebClient(@Value("${services.auth-service}") String authService) {
        return new AuthServiceClientImpl(WebClient
                .builder()
                .baseUrl(authService)
                .build());
    }
}
