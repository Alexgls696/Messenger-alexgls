package com.alexgls.springboot.indatabasecontentstorageservice.config;

import com.alexgls.springboot.indatabasecontentstorageservice.client.MessageStorageRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public MessageStorageRestClient messageStorageRestClient(@Value("${services.messages-service}") String messageStorageServiceUrl) {
        return new MessageStorageRestClient(RestClient
                .builder()
                .baseUrl(messageStorageServiceUrl)
                .build());
    }
}
