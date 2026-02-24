package com.alexgls.springboot.contentanalysisservice.config;

import com.alexgls.springboot.contentanalysisservice.exception.LoadFileToAiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableRetry
public class RetryConfig {
    @Bean
    public RetryTemplate aiRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(5)
                .exponentialBackoff(2000, 2.0, 10000)
                .retryOn(LoadFileToAiException.class)
                .build();
    }
}