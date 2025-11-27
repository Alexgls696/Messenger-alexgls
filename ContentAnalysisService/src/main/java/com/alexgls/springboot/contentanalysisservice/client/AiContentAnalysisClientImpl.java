package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.dto.AiContentAnalysisRequest;
import com.alexgls.springboot.contentanalysisservice.dto.AnalysisResponse;
import com.alexgls.springboot.contentanalysisservice.dto.LoadFileResponse;
import com.alexgls.springboot.contentanalysisservice.exception.GetOauthTokenFailedException;
import com.alexgls.springboot.contentanalysisservice.exception.LoadFileToAiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;


@RequiredArgsConstructor
public class AiContentAnalysisClientImpl implements AiContentAnalysisClient {

    private final RestClient restClient;

    @Override
    public AnalysisResponse analyzeTheFileById(AiContentAnalysisRequest aiContentAnalysisRequest) {
        try {
            return restClient
                    .post()
                    .uri("/chat/completions")
                    .headers(headers -> {
                        headers.add("Content-Type", "application/json");
                        headers.add("Accept", "application/json");
                    })
                    .body(aiContentAnalysisRequest)
                    .retrieve()
                    .body(AnalysisResponse.class);
        } catch (HttpClientErrorException.Unauthorized unauthorized) {
            throw unauthorized;
        } catch (HttpClientErrorException exception) {
            throw new GetOauthTokenFailedException("Не удалось проанализировать заданный файл. error: " + exception.getResponseBodyAsString());
        }
    }

    @Override
    public LoadFileResponse loadTheFile(Resource resource) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("purpose", "general");

            return restClient
                    .post()
                    .uri("/files")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(LoadFileResponse.class);
        } catch (HttpClientErrorException exception) {
            throw new LoadFileToAiException("Не удалось загрузить файл: " + exception.getResponseBodyAsString());
        }
    }
}
