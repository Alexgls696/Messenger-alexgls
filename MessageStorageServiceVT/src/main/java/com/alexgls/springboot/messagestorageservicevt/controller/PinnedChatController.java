package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.dto.chats.ChatDto;
import com.alexgls.springboot.messagestorageservicevt.entity.PinnedChat;
import com.alexgls.springboot.messagestorageservicevt.service.PinnedChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pinned-chats")
@Slf4j
public class PinnedChatController {

    private final PinnedChatService pinnedChatService;

    @GetMapping
    public List<PinnedChat> findAll(Authentication authentication) {
        int currentUserId = getSenderId(authentication);
        return pinnedChatService.getPinnedChatsByUserId(currentUserId);
    }

    @PostMapping
    public ResponseEntity<Void> createPinnedChat(@RequestParam("chatId") long chatId, Authentication authentication) {
        int currentUserId = getSenderId(authentication);
        pinnedChatService.savePinnedChat(chatId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete/{chatId}")
    public ChatDto deletePinnedChat(@PathVariable("chatId") long chatId, Authentication authentication) {
        int currentUserId = getSenderId(authentication);
        return pinnedChatService.deletePinnedChat(chatId, currentUserId);
    }

}
