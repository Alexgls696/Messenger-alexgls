package com.alexgls.springboot.searchdataservice.config;

import com.alexgls.springboot.searchdataservice.client.AuthServiceRestClient;
import com.alexgls.springboot.searchdataservice.client.AuthServiceRestClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientsConfig {

    @Bean
    public AuthServiceRestClient authServiceRestClient(@Value("${services.auth-service}") String authServiceUrl) {
        return new AuthServiceRestClientImpl(RestClient
                .builder()
                .baseUrl(authServiceUrl)
                .build());
    }
}
