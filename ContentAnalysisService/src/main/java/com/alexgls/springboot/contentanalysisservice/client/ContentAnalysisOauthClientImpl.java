package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.dto.AiContentAnalysisRequest;
import com.alexgls.springboot.contentanalysisservice.dto.OauthResponse;
import com.alexgls.springboot.contentanalysisservice.exception.GetOauthTokenFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ContentAnalysisOauthClientImpl implements ContentAnalysisOauthClient {

    private final RestClient restClient;

    @Value("${ai-services.gigachat.auth-token}")
    private String authorizationToken;

    @Override
    public OauthResponse getOauthTokenRequest() {
        try {
            String formdata = "scope=GIGACHAT_API_PERS";
            return restClient
                    .post()
                    .headers(httpHeaders -> {
                        httpHeaders.add("Authorization", "Bearer " + authorizationToken);
                        httpHeaders.add("Accept", "application/json");
                        httpHeaders.add("RqUID", UUID.randomUUID().toString());
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formdata)
                    .retrieve()
                    .body(OauthResponse.class);
        } catch (HttpClientErrorException exception) {
            throw new GetOauthTokenFailedException("Не удалось получить токен авторизации: " + exception.getResponseBodyAsString());
        }
    }
}
