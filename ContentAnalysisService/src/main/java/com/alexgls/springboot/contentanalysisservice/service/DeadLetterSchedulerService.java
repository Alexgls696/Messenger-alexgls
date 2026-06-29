package com.alexgls.springboot.contentanalysisservice.service;

import com.alexgls.springboot.contentanalysisservice.dto.AnalyseFileRequest;
import com.alexgls.springboot.contentanalysisservice.dto.DeadLetterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterSchedulerService {

    private final ConsumerFactory<String, DeadLetterRequest> dlqConsumerFactory;
    private final AiContentAnalysisService aiContentAnalysisService;

    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    // Переиспользуемый Consumer
    private Consumer<String, DeadLetterRequest> consumer;

    @PostConstruct
    public void init() {
        consumer = dlqConsumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("analysis-dlq-topic"));

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                consumer.poll(Duration.ZERO);
            } catch (Exception e) {
                log.warn("Heartbeat failed", e);
            }
        }, 0, 20, TimeUnit.SECONDS);

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
            // Читаем максимум 1 сообщение
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

            try {
                log.info("Ретрай файла {} из DLQ. Причина: {}",
                        dlqRequest.fileId(), dlqRequest.errorMessage());

                AnalyseFileRequest originalRequest = new AnalyseFileRequest(
                        dlqRequest.s3Key(),
                        dlqRequest.chatId(),
                        dlqRequest.fileId(),
                        dlqRequest.fileName()
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
}