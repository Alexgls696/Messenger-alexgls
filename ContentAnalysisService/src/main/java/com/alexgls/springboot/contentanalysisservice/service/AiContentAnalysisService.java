package com.alexgls.springboot.contentanalysisservice.service;

import com.alexgls.springboot.contentanalysisservice.client.AiContentAnalysisClient;
import com.alexgls.springboot.contentanalysisservice.client.ContentAnalysisOauthClient;
import com.alexgls.springboot.contentanalysisservice.dto.AiContentAnalysisRequest;
import com.alexgls.springboot.contentanalysisservice.dto.AnalysisResponse;
import com.alexgls.springboot.contentanalysisservice.dto.FileMetadata;
import com.alexgls.springboot.contentanalysisservice.exception.InvalidAnalysisRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
@Slf4j
public class AiContentAnalysisService {

    private String oauthToken;

    private final ContentAnalysisOauthClient contentAnalysisOauthClient;

    private final AiContentAnalysisClient aiContentAnalysisClient;

    @Async
    public CompletableFuture<FileMetadata> getAiContentAnalysisFromFile(String fileId) {
        if (oauthToken == null) {
            oauthToken = contentAnalysisOauthClient.getOauthTokenRequest().access_token();
        }
        var request = new AiContentAnalysisRequest(fileId);
        AnalysisResponse response = null;
        try {
            response = aiContentAnalysisClient.analyzeTheFileById(request, oauthToken);
        } catch (HttpClientErrorException.Unauthorized unauthorizedException) {
            oauthToken = contentAnalysisOauthClient.getOauthTokenRequest().access_token();
            response = aiContentAnalysisClient.analyzeTheFileById(request, oauthToken);
        }
        FileMetadata fileMetadata = convertAnalysisResponseToFileMetadata(response);
        return CompletableFuture.completedFuture(fileMetadata);

    }

    private FileMetadata convertAnalysisResponseToFileMetadata(AnalysisResponse analysisResponse) {
        String content = getContentFromAnalysisResponse(analysisResponse);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, FileMetadata.class);
        } catch (JsonProcessingException e) {
            throw new InvalidAnalysisRequestException("Не удалось преобразовать AnalysisResponse.choices[0].message.content в FileMetadata ");
        }
    }

    private String getContentFromAnalysisResponse(AnalysisResponse analysisResponse) {
        if (analysisResponse == null) {
            throw new InvalidAnalysisRequestException("AnalysisResponse был null");
        }
        if (analysisResponse.getChoices() == null || analysisResponse.getChoices().isEmpty()) {
            throw new InvalidAnalysisRequestException("AnalysisResponse.choices не содержит данных");
        }
        var choice = analysisResponse.getChoices().get(0);
        if (choice == null) {
            throw new InvalidAnalysisRequestException("AnalysisResponse.choices[0] не содержит данных");
        }
        var message = choice.getMessage();
        if (message == null) {
            throw new InvalidAnalysisRequestException("AnalysisResponse.choices[0].message не содержит данных");
        }
        String content = message.getContent();
        if (content == null) {
            throw new InvalidAnalysisRequestException("AnalysisResponse.choices[0].message.content не содержит данных");
        }
        return content;
    }


}
