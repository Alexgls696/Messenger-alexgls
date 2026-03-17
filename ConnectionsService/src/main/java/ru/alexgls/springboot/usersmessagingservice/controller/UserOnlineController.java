package ru.alexgls.springboot.usersmessagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.alexgls.springboot.usersmessagingservice.dto.CheckOnlineRequest;
import ru.alexgls.springboot.usersmessagingservice.service.PresenceService;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/online")
@RequiredArgsConstructor
@Slf4j
public class UserOnlineController {

    private final PresenceService presenceService;

    @PostMapping("/check-by-list")
    public Map<Integer, Boolean> checkUsersOnlineByList(@RequestBody CheckOnlineRequest checkOnlineRequest) {
        log.info("Check online request: {}", checkOnlineRequest);
        return presenceService.checkOnlineByList(checkOnlineRequest.usersIds());
    }

    @MessageMapping("/ping")
    public void ping(@Payload String ping, SimpMessageHeaderAccessor headerAccessor) {
        try {
            int userId = Integer.parseInt((String)headerAccessor
                    .getSessionAttributes()
                    .get("userId"));
            presenceService.setOnline(userId);
        } catch (RuntimeException exception) {
            log.warn("Failed to find userId and token", exception);
        }
    }

}
