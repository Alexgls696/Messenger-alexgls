package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.client.AuthWebClient;
import com.alexgls.springboot.messagestorageservice.dto.*;
import com.alexgls.springboot.messagestorageservice.service.ChatsService;
import com.alexgls.springboot.messagestorageservice.service.ParticipantsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alexgls.springboot.messagestorageservice.util.SecurityUtils.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatsController {
    private final ChatsService chatsService;
    private final AuthWebClient authWebClient;
    private final ParticipantsService participantsService;

    @Value("${values.page-size}")
    private Integer pageSize;

    @GetMapping("/{id}")
    public Mono<ChatDto> getChatById(@PathVariable int id, Authentication authentication) {
        int userId = getSenderId(authentication);
        return chatsService.findById(id, userId);
    }

    @GetMapping("/find-by-id/{page}")
    public Flux<ChatDto> findUserChatsById(
            @PathVariable("page") int page,
            Authentication authentication) {

        Integer userId = getSenderId(authentication);
        log.info("Find chats by user id: {}", userId);
        if (page < 0) {
            return Flux.error(new IllegalArgumentException("Page number cannot be negative"));
        }
        Pageable pageable = PageRequest.of(
                page,
                pageSize,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        return chatsService.findAllChatsByUserId(userId, pageable);
    }


    //Создание личного чата
    @PostMapping("/private/{receiverId}")
    public Mono<ChatDto> createPrivateChat(@PathVariable("receiverId") int id, Authentication authentication) {
        log.info("Create private chat, receiver id: {}", id);
        Integer userId = getSenderId(authentication);
        return chatsService.findOrCreatePrivateChat(userId, id);
    }

    @GetMapping("/find-chat-id-by-recipient-id/{id}")
    public Mono<Map<String, Integer>> findChatIdByRecipientId(@PathVariable("id") int id, Authentication authentication) {
        Integer userId = getSenderId(authentication);
        log.info("Find chat id by user id: {}", userId);
        return chatsService.findChatIdByRecipientId(id, userId)
                .map(chatId -> Map.of("chatId", chatId));
    }

    @GetMapping("/find-recipient-id-by-chat-id/{id}")
    public Mono<Integer> findRecipientIdByChatId(@PathVariable("id") int chatId, Authentication authentication) {
        log.info("Find recipient id by chat id: {}", chatId);
        Integer senderId = getSenderId(authentication);
        return chatsService.findRecipientIdByChatId(chatId, senderId);
    }

    @GetMapping("/find-recipient-by-private-chat-id/{id}")
    public Mono<GetUserDto> findUserByPrivateChatId(@PathVariable("id") int chatId, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Integer userId = getSenderId(authentication);
        String token = jwt.getTokenValue();
        log.info("Find user by chat id: {}", chatId);
        return chatsService.findRecipientIdByChatId(chatId, userId)
                .flatMap(recipientId -> authWebClient.findUserById(recipientId, token)).map(user -> {
                    log.info("Found user: {}", user);
                    return user;
                });

    }


    //Необходимо для загрузки участников групп
    @GetMapping("/{id}/participants")
    public Mono<List<GetUserDto>> findParticipantsByChatId(@PathVariable("id") int chatId, Authentication authentication) {
        log.info("Find participants by chat id: {}", chatId);
        String token = getToken(authentication);
        int userId = getSenderId(authentication);
        return participantsService.findAllByChatId(chatId, token, userId);
    }

    //Удаление участника группы
    @DeleteMapping("/{chatId}/participants/{userId}")
    public Mono<Void> deleteParticipantFromGroup(@PathVariable("chatId") int chatId, @PathVariable("userId") int userId, Authentication authentication) {
        log.info("Delete participant from chat id: {}", chatId);
        int currentUserId = getSenderId(authentication);
        String token = getToken(authentication);
        return participantsService.deleteParticipantFromGroup(chatId, userId, currentUserId, token);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteChatById(@PathVariable("id") int id, Authentication authentication) {
        int userId = getSenderId(authentication);
        log.info("Delete chat by id {} and userId: {}", id, userId);
        return chatsService.deleteChatById(id, userId)
                .then(Mono.just(ResponseEntity
                        .ok()
                        .build()));

    }

}
