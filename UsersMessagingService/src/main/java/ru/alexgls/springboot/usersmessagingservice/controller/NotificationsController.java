package ru.alexgls.springboot.usersmessagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.alexgls.springboot.usersmessagingservice.dto.notifications.NotificationDto;
import ru.alexgls.springboot.usersmessagingservice.dto.notifications.NotificationToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationsController {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "notifications-topic", groupId = "notification-consumers", containerFactory = "kafkaNotificationsConsumerFactory")
    public void listen(NotificationDto notificationDto) {
        log.info("Received notification: {}", notificationDto);
        NotificationToUser notificationToUser =  NotificationToUser.builder()
                .id(notificationDto.id())
                .title(notificationDto.title())
                .content(notificationDto.content())
                .createdAt(notificationDto.createdAt())
                .metadata(notificationDto.metadata())
                .imageId(notificationDto.imageId())
                .type(notificationDto.type())
                .build();

        for (var recipient : notificationDto.recipients()) {
            int userId = recipient.userId();
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", notificationToUser);
        }
    }
}
