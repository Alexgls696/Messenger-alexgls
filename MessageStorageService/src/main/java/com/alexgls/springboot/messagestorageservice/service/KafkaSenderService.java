package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.DeleteMessageResponse;
import com.alexgls.springboot.messagestorageservice.dto.MessageDto;
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

    private final KafkaTemplate<String, MessageDto> kafkaTemplate;

    private final KafkaTemplate<String, ReadMessagePayload> readMessageTemplate;

    private final KafkaTemplate<String, DeleteMessageResponse> deleteMessageTemplate;


    public void sendMessage(MessageDto createdMessageDto) {
        log.info("Try to sending message to kafka: {}", createdMessageDto);
        CompletableFuture<SendResult<String, MessageDto>> future = kafkaTemplate
                .send("events-message-created", createdMessageDto).toCompletableFuture();
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(throwable.getMessage(), throwable);
            }else{
                log.info("Message sent: {}", createdMessageDto);
            }
        });
    }

    public void sendReadMessagesToKafka(List<ReadMessagePayload> messages) {
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
