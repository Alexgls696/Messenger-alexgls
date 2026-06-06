package ru.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.alexgls.springboot.dto.BlacklistKafkaMessage;
import ru.alexgls.springboot.dto.CreateNotificationRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaSender {

    private final KafkaTemplate<String, CreateNotificationRequest> createNotificationTemplate;
    private final KafkaTemplate<String, BlacklistKafkaMessage> blacklistTemplate;

    public void sendNotification(CreateNotificationRequest request) {
        log.info("Send new notification request to kafka: {}", request);
        var futureResult = createNotificationTemplate.send("create-notifications-topic", request).toCompletableFuture();
        futureResult.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(throwable.getMessage(), throwable);
            } else {
                log.info("Successfully sent notification to kafka: {}", request);
            }
        });
    }

    public void sendBlacklistMessage(int currentUser, int targetUser, boolean lock) {
        log.info("Send lock notification request to kafka: current: {} target: {} lock: {}", currentUser, targetUser, lock);
        BlacklistKafkaMessage blacklistKafkaMessage = new BlacklistKafkaMessage(currentUser, targetUser, lock);
        var futureResult = blacklistTemplate.send("blacklist-topic", blacklistKafkaMessage).toCompletableFuture();
        futureResult.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(throwable.getMessage(), throwable);
            } else {
                log.info("Successfully sent lock notification to kafka: {}", blacklistKafkaMessage);
            }
        });
    }
}
