package com.alexgls.springboot.contentanalysisservice.service;

import com.alexgls.springboot.contentanalysisservice.dto.AnalyseFileRequest;
import com.alexgls.springboot.contentanalysisservice.dto.DeadLetterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterSchedulerService {

    private final ConsumerFactory<String, DeadLetterRequest> dlqConsumerFactory;
    private final AiContentAnalysisService aiContentAnalysisService;
    private final KafkaTemplate<String, DeadLetterRequest> permanentFailureKafkaTemplate;

    @Value("${dlq.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${dlq.retry.max-age-hours:24}")
    private int maxAgeHours;

    private Consumer<String, DeadLetterRequest> consumer;

    @PostConstruct
    public void init() {
        consumer = dlqConsumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("analysis-dlq-topic"));
        log.info("DLQ Consumer инициализирован и подписан на топик");
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
            log.info("DLQ Consumer закрыт");
        }
    }

    @Scheduled(fixedDelayString = "${dlq.retry.interval-ms:60000}")
    public void retryOneRequestFromDlq() {
        try {
            ConsumerRecords<String, DeadLetterRequest> records = consumer.poll(Duration.ofSeconds(2));

            if (records.isEmpty()) {
                return;
            }

            var record = records.iterator().next();

            if (record.value() == null) {
                log.warn("Получено сообщение с null value из DLQ, пропускаем");
                consumer.commitSync();
                return;
            }

            DeadLetterRequest dlqRequest = record.value();

            // Проверка 1: Лимит попыток
            if (dlqRequest.retryCount() >= maxRetryAttempts) {
                log.error("Файл {} исчерпал лимит retry ({} попыток). Помечаем как 'permanent failure'.",
                        dlqRequest.fileId(), maxRetryAttempts);
                moveToPermanentFailure(dlqRequest);
                consumer.commitSync();
                return;
            }

            // Проверка 2: Возраст сообщения
            long ageHours = (System.currentTimeMillis() - dlqRequest.firstFailureTime()) / (1000 * 60 * 60);
            if (ageHours > maxAgeHours) {
                log.error("Файл {} слишком старый ({} часов). Помечаем как 'permanent failure'.",
                        dlqRequest.fileId(), ageHours);
                moveToPermanentFailure(dlqRequest);
                consumer.commitSync();
                return;
            }

            try {
                log.info("Ретрай файла {} из DLQ (попытка {}/{}). Причина: {}",
                        dlqRequest.fileId(), dlqRequest.retryCount() + 1, maxRetryAttempts,
                        dlqRequest.errorMessage());

                AnalyseFileRequest originalRequest = new AnalyseFileRequest(
                        dlqRequest.s3Key(),
                        dlqRequest.chatId(),
                        dlqRequest.fileId(),
                        dlqRequest.fileName(),
                        dlqRequest.retryCount() + 1
                );

                aiContentAnalysisService.analyseFile(originalRequest);
                consumer.commitSync();

                log.info("Файл {} отправлен на повторный анализ", dlqRequest.fileId());

            } catch (Exception e) {
                log.error("Ошибка при ретрае файла {}", dlqRequest.fileId(), e);
                consumer.commitSync();
            }

        } catch (Exception e) {
            log.error("Ошибка при чтении DLQ", e);

            try {
                consumer.close();
                consumer = dlqConsumerFactory.createConsumer();
                consumer.subscribe(Collections.singletonList("analysis-dlq-topic"));
                log.info("DLQ Consumer пересоздан после ошибки");
            } catch (Exception ex) {
                log.error("Не удалось пересоздать DLQ Consumer", ex);
            }
        }
    }

    private void moveToPermanentFailure(DeadLetterRequest request) {
        log.warn("Файл {} помечен как permanent failure", request.fileId());
        permanentFailureKafkaTemplate.send("permanent-failure-topic", request);
    }
}