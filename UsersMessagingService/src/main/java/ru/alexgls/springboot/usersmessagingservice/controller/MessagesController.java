package ru.alexgls.springboot.usersmessagingservice.controller;

import ru.alexgls.springboot.usersmessagingservice.service.MessagingService;
import ru.alexgls.springboot.usersmessagingservice.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MessagesController {

    private final MessagingService messagingService;
    @MessageMapping("/chat.send")
    public void sendChatMessage(@Payload ChatMessage chatMessage, Principal principal) {
        log.info("Try to send message to kafka: {}", chatMessage);
        if(chatMessage.getContent().isEmpty() && chatMessage.getAttachments().isEmpty()){
            return;
        }
        messagingService.sendMessage(chatMessage, principal);
    }

}
