package com.alexgls.springboot.contentanalysisservice.config;

import com.alexgls.springboot.contentanalysisservice.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientsConfig {

    @Bean
    public ContentAnalysisOauthClient contentAnalysisOauthClient(@Value("${ai-services.gigachat.auth-url}") String oauthUrl) {
        return new ContentAnalysisOauthClientImpl(RestClient
                .builder()
                .baseUrl(oauthUrl)
                .build());
    }

    @Bean
    public AiContentAnalysisClient contentAnalysisClient(@Value("${ai-services.gigachat.ai-url}") String AIUrl, AuthInterceptor authInterceptor) {
        return new AiContentAnalysisClientImpl(RestClient
                .builder()
                .baseUrl(AIUrl)
                .requestInterceptor(authInterceptor)
                .build());
    }
}
