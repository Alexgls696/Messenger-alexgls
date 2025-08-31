package ru.alexgls.springboot.usersmessagingservice.controller;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.alexgls.springboot.usersmessagingservice.dto.MessageDto;
import ru.alexgls.springboot.usersmessagingservice.dto.ReadMessagePayload;
import ru.alexgls.springboot.usersmessagingservice.service.MessagingService;
import ru.alexgls.springboot.usersmessagingservice.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@Slf4j
public class MessagesController {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendChatMessage(@Payload ChatMessage chatMessage, Principal principal) {
        log.info("Try to send message to kafka: {}", chatMessage);
        if(chatMessage.getContent().isEmpty() && chatMessage.getAttachments().isEmpty()){
            return;
        }
        messagingService.sendMessage(chatMessage, principal);
    }

    @KafkaListener(topics = "events-message-created", groupId = "event-message-group", containerFactory = "kafkaMessageListenerContainerFactory")
    public void listen(MessageDto createdMessageDto) {
        log.info("Received message, which was saved to database: {}", createdMessageDto);
        log.info("Try to send message to client: {}", createdMessageDto);
        messagingTemplate.convertAndSendToUser(String.valueOf(createdMessageDto.getRecipientId()), "/queue/messages", createdMessageDto);
        messagingTemplate.convertAndSendToUser(String.valueOf(createdMessageDto.getSenderId()), "/queue/messages", createdMessageDto);
    }

    @KafkaListener(topics = "read-message-topic", groupId = "message-read-group", containerFactory = "kafkaReadMessagesConsumerFactory")
    public void listenReadMessage(ReadMessagePayload payload) {
        if (payload == null) {
            return;
        }
        log.info("Received a read message event: {}", payload);
        var notificationPayload = Map.of(
                "chatId", payload.chatId(),
                "messageIds", List.of(payload.messageId())
        );

        messagingTemplate.convertAndSendToUser(
                String.valueOf(payload.senderId()),
                "/queue/read-status",
                notificationPayload
        );

        log.info("Sent read status for chat {} (messageId: {}) to user {}",
                payload.chatId(), payload.messageId(), payload.senderId());
    }

}
