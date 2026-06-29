package com.alexgls.springboot.contentanalysisservice.config;

import com.alexgls.springboot.contentanalysisservice.exception.GetOauthTokenFailedException;
import com.alexgls.springboot.contentanalysisservice.exception.InvalidAnalysisRequestException;
import com.alexgls.springboot.contentanalysisservice.exception.LoadFileToAiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

@Configuration
@EnableRetry
public class RetryConfig {

    @Bean
    public RetryTemplate aiRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(5)
                .exponentialBackoff(2000, 2.0, 10000)
                .retryOn(List.of(
                        LoadFileToAiException.class,           // Ошибки загрузки файла (4xx, 5xx, сеть)
                        GetOauthTokenFailedException.class,    // Ошибки анализа (4xx, 5xx, сеть)
                        ResourceAccessException.class,         // Сетевые ошибки (на случай, если вылетят где-то еще)
                        InvalidAnalysisRequestException.class) // Ошибки парсинга ответа от AI
                )
                .build();
    }
}