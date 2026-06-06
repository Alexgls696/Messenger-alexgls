package com.alexgls.springboot.searchdataservice.controller;

import com.alexgls.springboot.searchdataservice.dto.GetUserDto;
import com.alexgls.springboot.searchdataservice.dto.MessageDto;
import com.alexgls.springboot.searchdataservice.dto.SearchMessageInChatRequest;
import com.alexgls.springboot.searchdataservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/users/by-key/{key}")
    public Iterable<GetUserDto> findAllUsersByUsername(Authentication authentication, @PathVariable String key) {
        String token = getToken(authentication);
        log.info("find users by username {}", key);
        return searchService.findAllUsersByKey(key, token);
    }

    @GetMapping("/users/from-chats")
    public Iterable<GetUserDto> findAllUsersFromChats(Authentication authentication) {
        String token = getToken(authentication);
        log.info("find users from users chats");
        return searchService.findAllUsersFromChats(token);
    }

    @PostMapping("/messages/find-by-content-in-chat")
    public Iterable<MessageDto> findAllMessagesByContentInChat(@RequestBody SearchMessageInChatRequest request, Authentication authentication) {
        String token = getToken(authentication);
        log.info("find messages by content in-chat {}", request);
        return searchService.findMessagesByContentInChat(request, token);
    }

    private String getToken(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getTokenValue();
    }

}
