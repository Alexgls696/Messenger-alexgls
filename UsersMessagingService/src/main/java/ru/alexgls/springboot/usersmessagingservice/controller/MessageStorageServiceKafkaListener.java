package ru.alexgls.springboot.usersmessagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.alexgls.springboot.usersmessagingservice.dto.DeleteMessageResponse;
import ru.alexgls.springboot.usersmessagingservice.dto.DeleteMessageResponseToUser;
import ru.alexgls.springboot.usersmessagingservice.dto.MessageDto;
import ru.alexgls.springboot.usersmessagingservice.dto.ReadMessagePayload;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MessageStorageServiceKafkaListener {

    private final SimpMessagingTemplate messagingTemplate;

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


    @KafkaListener(topics = "delete-message-topic", groupId = "delete-message-group", containerFactory = "kafkaDeleteMessagesConsumerFactory")
    public void listenDeleteMessage(DeleteMessageResponse deleteMessageResponse) {
        log.info("Received a delete message event: {}", deleteMessageResponse);
        DeleteMessageResponseToUser deleteMessageResponseToUser = new DeleteMessageResponseToUser(deleteMessageResponse.messagesId(),
                deleteMessageResponse.senderId(),
                deleteMessageResponse.chatId());
        if (deleteMessageResponse.forAll()) {
            for (var recipientId : deleteMessageResponse.recipientsId()) {
                log.info("Deleting messages with for user with id {}", recipientId);
                messagingTemplate.convertAndSendToUser(String.valueOf(recipientId), "/queue/delete-event", deleteMessageResponseToUser);
            }
        } else {
            messagingTemplate.convertAndSendToUser(String.valueOf(deleteMessageResponse.senderId()), "/queue/delete-event", deleteMessageResponseToUser);
        }

    }
}
