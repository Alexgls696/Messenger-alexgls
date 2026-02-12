package com.alexgls.springboot.messagestorageservicevt.config;

import com.alexgls.springboot.messagestorageservicevt.utils.AuthHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public AuthHttpClient authHttpClient(@Value("${services.auth-service}") String authServiceUrl) {
        return new AuthHttpClient(RestClient
                .builder()
                .baseUrl(authServiceUrl)
                .build());
    }
}
