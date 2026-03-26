package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.dto.chats.ChatDto;
import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.dto.chats.GroupParticipantsDto;
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
    private final ParticipantsService participantsService;

    @Value("${values.page-size}")
    private Integer pageSize;

    @GetMapping("/{id}")
    public ChatDto getChatById(@PathVariable("id") int chatId, Authentication authentication) {
        int userId = getSenderId(authentication);
        return chatsService.findChatById(chatId, userId);
    }

    @GetMapping("/by-user/{id}")
    public ChatDto getChatByUserId(@PathVariable("id") int userId, Authentication authentication) {
        int senderId = getSenderId(authentication);
        return chatsService.findPrivateChat(senderId, userId);
    }

    @GetMapping("/find-all/{page}")
    public List<ChatDto> findUserChatsById(
            @PathVariable("page") int page,
            Authentication authentication) {

        Integer userId = getSenderId(authentication);
        String token = getToken(authentication);
        log.info("Find chats by user id: {}", userId);
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        Pageable pageable = PageRequest.of(
                page,
                pageSize,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );
        return chatsService.findAllChats(userId, token, pageable);
    }

    //Создание личного чата
    @PostMapping("/private/{receiverId}")
    public ChatDto createPrivateChat(@PathVariable("receiverId") int id, Authentication authentication) {
        log.info("Find or create private chat, receiver id: {}", id);
        Integer userId = getSenderId(authentication);
        return chatsService.findPrivateChat(userId, id);
    }

    @GetMapping("/find-recipient-by-private-chat-id/{id}")
    public GetUserDto findUserByPrivateChatId(@PathVariable("id") int chatId, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Integer userId = getSenderId(authentication);
        String token = jwt.getTokenValue();
        return chatsService.findRecipientIdByChatId(chatId, userId, token);
    }

    //Необходимо для загрузки участников групп и права доступа текущего пользователя
    @GetMapping("/{id}/participants")
    public GroupParticipantsDto findParticipantsByChatId(@PathVariable("id") int chatId, Authentication authentication) {
        log.info("Find participants by chat id: {}", chatId);
        String token = getToken(authentication);
        int userId = getSenderId(authentication);
        return participantsService.findAllByChatId(chatId, token, userId);
    }

    @GetMapping("/{chatId}/participants/exists/{userId}")
    public ResponseEntity<Map<String, Boolean>> exists(@PathVariable("chatId") int chatId, @PathVariable("userId") int userId) {
        log.info("Check user {} in chat {} participants", userId, chatId);
        boolean exists = participantsService.existsByChatIdAndUserId(chatId,userId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/{id}/participants-ids")
    public List<Integer> findAllParticipantsIdsByChatId(@PathVariable("id") long chatId) {
        return participantsService.findUserIdsByChatId(chatId);
    }

    /**
     * Запрос пользователей, с которыми был чат.
     *
     * @param authentication Содержит информацию о текущем пользователе.
     * @return Список id пользователей
     */
    @GetMapping("/search-users")
    public Iterable<Integer> findAllUsersWhoHadChatWith(Authentication authentication) {
        Integer userId = getSenderId(authentication);
        return participantsService.findAllUsersWhoHadChatWith(userId);
    }

    @GetMapping("/search-users/{id}")
    public Iterable<Integer> findAllUsersWhoHadChatWithById(@PathVariable("id") int userId) {
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
