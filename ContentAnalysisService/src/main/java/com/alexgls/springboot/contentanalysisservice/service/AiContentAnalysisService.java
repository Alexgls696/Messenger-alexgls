package com.alexgls.springboot.contentanalysisservice.service;

import com.alexgls.springboot.contentanalysisservice.client.AiContentAnalysisClient;
import com.alexgls.springboot.contentanalysisservice.client.S3VkCloudClient;
import com.alexgls.springboot.contentanalysisservice.dto.*;
import com.alexgls.springboot.contentanalysisservice.exception.InvalidAnalysisRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class AiContentAnalysisService {

    private final ObjectMapper objectMapper;
    private final S3VkCloudClient s3VkCloudClient;
    private final AiContentAnalysisClient aiContentAnalysisClient;
    private final KafkaTemplate<String, ElasticSearchStorageServiceRequest> kafkaTemplate;
    private final KafkaTemplate<String, DeadLetterRequest> dlqKafkaTemplate;
    private final RetryTemplate retryTemplate;

    private final Semaphore analysisSemaphore;

    public AiContentAnalysisService(
            ObjectMapper objectMapper,
            S3VkCloudClient s3VkCloudClient,
            AiContentAnalysisClient aiContentAnalysisClient,
            KafkaTemplate<String, ElasticSearchStorageServiceRequest> kafkaTemplate,
            KafkaTemplate<String, DeadLetterRequest> dlqKafkaTemplate,
            RetryTemplate retryTemplate,
            @Value("${ai-services.countRequestsToAi}") int countRequestsToAi) {

        this.objectMapper = objectMapper;
        this.s3VkCloudClient = s3VkCloudClient;
        this.aiContentAnalysisClient = aiContentAnalysisClient;
        this.kafkaTemplate = kafkaTemplate;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.retryTemplate = retryTemplate;

        this.analysisSemaphore = new Semaphore(countRequestsToAi);
        log.info("Глобальный семафор для анализа инициализирован с лимитом: {} запрос(ов)", countRequestsToAi);
    }

    @Async("analysisTaskExecutor")
    public CompletableFuture<Void> analyseFile(AnalyseFileRequest analyseFileRequest) {

        try {
            if (!analysisSemaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                log.warn("Очередь переполнена. Файл {} отправлен в DLQ из-за таймаута ожидания.",
                        analyseFileRequest.getFileId());
                sendToDeadLetterQueue(analyseFileRequest, new TimeoutException("Таймаут ожидания в очереди"));
                return CompletableFuture.completedFuture(null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }

        try {
            byte[] fileBytes;
            try (InputStream inputStream = s3VkCloudClient.downloadFile(analyseFileRequest.getKey())) {
                fileBytes = inputStream.readAllBytes();
            } catch (IOException e) {
                log.error("Не удалось скачать файл из S3 для анализа", e);
                sendToDeadLetterQueue(analyseFileRequest, e);
                return CompletableFuture.failedFuture(e);
            }

            retryTemplate.execute(
                    context -> {
                        log.info("Попытка анализа файла {}, попытка №{}",
                                analyseFileRequest.getFileId(), context.getRetryCount() + 1);

                        Resource resource = new ByteArrayResource(fileBytes) {
                            @Override
                            public String getFilename() {
                                return analyseFileRequest.getFileName();
                            }
                        };

                        LoadFileResponse loadFileResponse = aiContentAnalysisClient.loadTheFile(resource);
                        var analysisResponse = aiContentAnalysisClient.analyzeTheFileById(
                                new AiContentAnalysisRequest(loadFileResponse.id())
                        );

                        var metadata = convertAnalysisResponseToFileMetadata(analysisResponse);
                        sendMetadataToKafka(metadata, analyseFileRequest.getChatId(), analyseFileRequest.getFileId());

                        return null;
                    },
                    context -> {
                        Throwable lastError = context.getLastThrowable();
                        log.error("Все попытки ретраев исчерпаны для файла {}. Отправляем в DLQ.",
                                analyseFileRequest.getFileId(), lastError);
                        sendToDeadLetterQueue(analyseFileRequest, lastError);
                        return null;
                    }
            );

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Критическая ошибка при анализе файла {}", analyseFileRequest.getFileId(), e);
            sendToDeadLetterQueue(analyseFileRequest, e);
            return CompletableFuture.failedFuture(e);
        } finally {
            analysisSemaphore.release();
        }
    }

    private void sendMetadataToKafka(FileMetadataDto fileMetadata, int chatId, int fileId) {
        ElasticSearchStorageServiceRequest request = new ElasticSearchStorageServiceRequest(fileMetadata, chatId, fileId);
        CompletableFuture<SendResult<String, ElasticSearchStorageServiceRequest>> future =
                kafkaTemplate.send("metadata-topic", request);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Error when sending via kafka", throwable.getMessage());
            } else {
                log.info("Successfully sent via kafka");
            }
        });
    }

    private void sendToDeadLetterQueue(AnalyseFileRequest request, Throwable error) {
        DeadLetterRequest dlqRequest = new DeadLetterRequest(
                request.getFileId(),
                request.getChatId(),
                request.getKey(),
                request.getFileName(),
                error.getClass().getName(),
                error.getMessage()
        );

        CompletableFuture<SendResult<String, DeadLetterRequest>> future =
                dlqKafkaTemplate.send("analysis-dlq-topic", dlqRequest);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Критическая ошибка: не удалось отправить файл в DLQ!", throwable);
            } else {
                log.info("Файл {} успешно отправлен в Dead Letter Queue", request.getFileId());
            }
        });
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