package com.alexgls.springboot.contentanalysisservice.service;

import com.alexgls.springboot.contentanalysisservice.client.AiContentAnalysisClient;
import com.alexgls.springboot.contentanalysisservice.dto.*;

import com.alexgls.springboot.contentanalysisservice.exception.InvalidAnalysisRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
@Slf4j
public class AiContentAnalysisService {

    private final ObjectMapper objectMapper;

    private final AiContentAnalysisClient aiContentAnalysisClient;

    private final KafkaTemplate<String, ElasticSearchStorageServiceRequest> kafkaTemplate;


    @Async
    public CompletableFuture<Void> analyseFile(Resource safeResource, int chatId, int fileId) {
        return CompletableFuture.supplyAsync(() -> aiContentAnalysisClient.loadTheFile(safeResource))
                .thenApply(loadFileResponse -> new AiContentAnalysisRequest(loadFileResponse.id()))
                .thenApply(aiContentAnalysisClient::analyzeTheFileById)
                .thenApply(this::convertAnalysisResponseToFileMetadata)
                .thenAccept(metadata -> sendMetadataToKafka(metadata, chatId, fileId));
    }

    private void sendMetadataToKafka(FileMetadataDto fileMetadata, int chatId, int fileId) {
        ElasticSearchStorageServiceRequest request = new ElasticSearchStorageServiceRequest(fileMetadata, chatId, fileId);
        CompletableFuture<SendResult<String, ElasticSearchStorageServiceRequest>> future = kafkaTemplate.send("metadata-topic", request);
        future.whenComplete((result, throwable) -> handleKafkaResultThrowable(throwable));
    }

    private void handleKafkaResultThrowable(Throwable throwable) {
        if (throwable != null) {
            log.error("Error when sending via kafka", throwable.getMessage());
        } else {
            log.info("Successfully sent via kafka");
        }
    }

    private FileMetadataDto convertAnalysisResponseToFileMetadata(AnalysisResponse analysisResponse) {
        String content = getContentFromAnalysisResponse(analysisResponse);
        try {
            return objectMapper.readValue(content, FileMetadataDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Не удалось преобразовать AnalysisResponse.choices[0].message.content в FileMetadata {}", e.getMessage());
            throw new InvalidAnalysisRequestException("Не удалось преобразовать AnalysisResponse.choices[0].message.content в FileMetadata " + e.getMessage());
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
