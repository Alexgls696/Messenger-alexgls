package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.dto.AiContentAnalysisRequest;
import com.alexgls.springboot.contentanalysisservice.dto.AnalysisResponse;
import com.alexgls.springboot.contentanalysisservice.exception.GetOauthTokenFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;


@RequiredArgsConstructor
public class AiContentAnalysisClientImpl implements AiContentAnalysisClient {

    private final RestClient restClient;

    @Override
    public AnalysisResponse analyzeTheFileById(AiContentAnalysisRequest aiContentAnalysisRequest, String token) {
        try {
            return restClient
                    .post()
                    .uri("/chat/completions")
                    .headers(headers -> {
                        headers.add("Authorization", "Bearer " + token);
                        headers.add("Content-Type", "application/json");
                        headers.add("Accept", "application/json");
                    })
                    .body(aiContentAnalysisRequest)
                    .retrieve()
                    .body(AnalysisResponse.class);
        }
        catch (HttpClientErrorException.Unauthorized unauthorized) {
            throw unauthorized;
        } catch (HttpClientErrorException exception) {
            throw new GetOauthTokenFailedException("Не удалось проанализировать заданный файл. error: " + exception.getResponseBodyAsString());
        }
    }
}
