package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.client.AuthWebClient;
import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.dto.CreateGroupDto;
import com.alexgls.springboot.messagestorageservice.dto.GetUserDto;
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
        log.info("Create private chat: {}", id);
        Integer userId = getSenderId(authentication);
        return chatsService.findOrCreatePrivateChat(userId, id);
    }

    @PostMapping("/group")
    public Mono<ChatDto> createGroupChat(@RequestBody CreateGroupDto createGroupDto, Authentication authentication) {
        Integer id = getSenderId(authentication);
        log.info("Create group chat: {}", id);
        return chatsService.createGroup(createGroupDto, id);

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

    @GetMapping("/{id}/participants")
    public Flux<GetUserDto> findParticipantsByChatId(@PathVariable("id") int chatId, Authentication authentication) {
        log.info("Find participants by chat id: {}", chatId);
        String token = getToken(authentication);
        return participantsService.findUserIdsByChatId(chatId)
                .flatMap(id -> authWebClient.findUserById(id, token));
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

    private Integer getSenderId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }

    private String getToken(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getTokenValue();
    }
}
