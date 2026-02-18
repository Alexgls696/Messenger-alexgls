package com.alexgls.springboot.notificationsservice.service;

import com.alexgls.springboot.notificationsservice.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaSender {

    private final KafkaTemplate<String, NotificationDto> notificationKafkaTemplate;

    public void sendNotification(NotificationDto notificationDto) {
        log.info("Sending new notification to kafka: {}", notificationDto);
        CompletableFuture<SendResult<String, NotificationDto>> futureResult = notificationKafkaTemplate.send("notifications-topic", notificationDto)
                .toCompletableFuture();
        futureResult.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                log.error("Error sending notification", throwable);
            } else {
                log.info("Notification sent successfully");
            }
        });
    }
}
