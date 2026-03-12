package com.alexgls.springboot.messagestorageservicevt.config;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClientImpl;
import com.alexgls.springboot.messagestorageservicevt.client.ConnectionsServiceRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientsConfig {
    @Bean
    public AuthRestClient authRestClient(@Value("${services.auth-service}") String authService) {
        return new AuthRestClientImpl(RestClient
                .builder()
                .baseUrl(authService)
                .build());
    }

    @Bean
    public ConnectionsServiceRestClient connectionsServiceRestClient(@Value("${services.connections-service}") String connectionsService) {
        return new ConnectionsServiceRestClient(RestClient
                .builder()
                .baseUrl(connectionsService)
                .build());
    }
}
