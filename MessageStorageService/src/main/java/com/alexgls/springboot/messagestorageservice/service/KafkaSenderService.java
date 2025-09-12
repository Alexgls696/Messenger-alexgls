package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.DeleteMessageResponse;
import com.alexgls.springboot.messagestorageservice.dto.ReadMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaSenderService {

    private final KafkaTemplate<String, ReadMessagePayload> readMessageTemplate;

    private final KafkaTemplate<String, DeleteMessageResponse> deleteMessageTemplate;

    public void sendMessagesToKafka(List<ReadMessagePayload> messages, long count) {
        log.info("Count of updated messages {}", count);
        messages.forEach(message -> {
            CompletableFuture<SendResult<String, ReadMessagePayload>> futureResult = readMessageTemplate.send("read-message-topic", message).toCompletableFuture();
            futureResult.whenComplete((result, throwable) -> {
                handleKafkaResultThrowable(throwable);
            });
        });

    }

    public void sendDeleteEventMessagesToKafka(DeleteMessageResponse deleteMessageResponse) {
        log.info("Delete messages event sending via kafka {}", deleteMessageResponse);
        CompletableFuture<SendResult<String, DeleteMessageResponse>> futureResult = deleteMessageTemplate.send("delete-message-topic", deleteMessageResponse).toCompletableFuture();
        futureResult.whenComplete((result, throwable) -> {
            handleKafkaResultThrowable(throwable);
        });
    }

    private void handleKafkaResultThrowable(Throwable throwable) {
        if (throwable != null) {
            log.error("Error when sending via kafka", throwable.getMessage());
        } else {
            log.info("Successfully sent via kafka");
        }
    }

}
