package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.dto.DeleteMessageRequest;
import com.alexgls.springboot.messagestorageservice.dto.MessageDto;
import com.alexgls.springboot.messagestorageservice.dto.ReadMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.SearchMessageInChatRequest;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.service.KafkaSenderService;
import com.alexgls.springboot.messagestorageservice.service.MessagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessagesController {

    private final MessagesService messagesService;

    private final KafkaSenderService kafkaSenderService;


    @GetMapping
    public Flux<Message> findMessagesByChatId(
            @RequestParam("chatId") int chatId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int pageSize, Authentication authentication) {
        int currentUserId = getCurrentUserId(authentication);
        log.info("findMessagesByChatId chatId={}, page={}, size={}", chatId, page, pageSize);
        return messagesService.getMessagesByChatId(chatId, page * pageSize, pageSize, currentUserId)
                .map(message -> {
                    log.info("findMessagesByChatId message: {} ", message);
                    return message;
                });
    }

    @PostMapping("/find-by-content-in-chat")
    public Flux<MessageDto> findMessagesByChatId(@RequestBody SearchMessageInChatRequest request) {
        log.info("find messages by content in the chat : {}", request);
        return messagesService.findMessagesByContent(request);
    }

    @PostMapping("/read-messages")
    public Mono<Long> readMessagesList(@RequestBody List<ReadMessagePayload> messages, Authentication authentication) {
        int currentUserId = getCurrentUserId(authentication);
        final List<ReadMessagePayload> filteredMessages = messages.stream().filter(message -> message.senderId() != currentUserId).toList();
        return Mono.just(filteredMessages)
                .flatMap(messagesList -> {
                    log.info("Read messages from payload... {}", messagesList);
                    return messagesService.readMessagesByList(messagesList);
                }).flatMap(count -> {
                    kafkaSenderService.sendMessagesToKafka(filteredMessages, count);
                    return Mono.just(count);
                });
    }

    @DeleteMapping
    public Mono<Void> deleteMessage(@RequestBody DeleteMessageRequest deleteMessageRequest, Authentication authentication) {
        int currentUserId = getCurrentUserId(authentication);
        log.info("Try to delete messages: {} ", deleteMessageRequest);
        return messagesService.deleteById(deleteMessageRequest, currentUserId)
                .flatMap(response -> {
                    kafkaSenderService.sendDeleteEventMessagesToKafka(response);
                    return Mono.empty();
                });
    }

    @GetMapping("/encrypt_all")
    public Flux<MessageDto> encryptAllMessages() {
        log.info("Encrypt all messages");
        return messagesService.encryptAllMessages();
    }

    private Integer getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }


}
