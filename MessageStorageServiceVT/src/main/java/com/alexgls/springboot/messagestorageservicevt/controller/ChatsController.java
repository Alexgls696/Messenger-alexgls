package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.dto.ChatDto;
import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.service.ChatsService;
import com.alexgls.springboot.messagestorageservicevt.service.ParticipantsService;
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

import java.util.List;
import java.util.Map;

import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.getSenderId;
import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.getToken;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatsController {

    private final ChatsService chatsService;
    private final AuthRestClient authRestClient;
    private final ParticipantsService participantsService;

    @Value("${values.page-size}")
    private Integer pageSize;

    @GetMapping("/{id}")
    public ChatDto getChatById(@PathVariable int id, Authentication authentication) {
        int userId = getSenderId(authentication);
        return chatsService.findById(id, userId);
    }

    @GetMapping("/find-by-id/{page}")
    public List<ChatDto> findUserChatsById(
            @PathVariable("page") int page,
            Authentication authentication) {

        Integer userId = getSenderId(authentication);
        log.info("Find chats by user id: {}", userId);
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
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
    public ChatDto createPrivateChat(@PathVariable("receiverId") int id, Authentication authentication) {
        log.info("Find or create private chat, receiver id: {}", id);
        Integer userId = getSenderId(authentication);
        return chatsService.findOrCreatePrivateChat(userId, id);
    }

    //unused
    @GetMapping("/find-chat-id-by-recipient-id/{id}")
    public ResponseEntity<Map<String, Integer>> findChatIdByRecipientId(@PathVariable("id") int id, Authentication authentication) {
        Integer userId = getSenderId(authentication);
        log.info("Find chat id by user id: {}", userId);
        int chatId = chatsService.findChatIdByRecipientId(id, userId);
        return ResponseEntity.ok(Map.of("chatId", chatId));
    }

    //unused
    @GetMapping("/find-recipient-id-by-chat-id/{id}")
    public Integer findRecipientIdByChatId(@PathVariable("id") int chatId, Authentication authentication) {
        log.info("Find recipient id by chat id: {}", chatId);
        Integer senderId = getSenderId(authentication);
        return chatsService.findRecipientIdByChatId(chatId, senderId);
    }

    @GetMapping("/find-recipient-by-private-chat-id/{id}")
    public GetUserDto findUserByPrivateChatId(@PathVariable("id") int chatId, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Integer userId = getSenderId(authentication);
        String token = jwt.getTokenValue();
        Integer recipientId = chatsService.findRecipientIdByChatId(chatId, userId);
        var user = authRestClient.findUserById(recipientId, token);
        return user;
    }

    //Необходимо для загрузки участников групп
    @GetMapping("/{id}/participants")
    public List<GetUserDto> findParticipantsByChatId(@PathVariable("id") int chatId, Authentication authentication) {
        log.info("Find participants by chat id: {}", chatId);
        String token = getToken(authentication);
        int userId = getSenderId(authentication);
        return participantsService.findAllByChatId(chatId, token, userId);
    }


    /**
     * Запрос пользователей, с которыми был чат.
     * @param authentication Содержит информацию о текущем пользователе.
     * @return Список id пользователей
     */
    @GetMapping("/search-users")
    public Iterable<Integer> findAllUsersWhoHadChatWith(Authentication authentication) {
        Integer userId = getSenderId(authentication);
        log.info("Find all users who have had chat: {}", userId);
        return participantsService.findAllUsersWhoHadChatWith(userId);
    }

    //Удаление участника группы
    @DeleteMapping("/{chatId}/participants/{userId}")
    public void deleteParticipantFromGroup(@PathVariable("chatId") int chatId, @PathVariable("userId") int userId, Authentication authentication) {
        log.info("Delete participant from chat id: {}", chatId);
        int currentUserId = getSenderId(authentication);
        String token = getToken(authentication);
        participantsService.deleteParticipantFromGroup(chatId, userId, currentUserId, token);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatById(@PathVariable("id") int id, Authentication authentication) {
        int userId = getSenderId(authentication);
        log.info("Delete chat by id {} and userId: {}", id, userId);
        chatsService.deleteChatById(id, userId);
        return ResponseEntity
                .noContent()
                .build();
    }
}
