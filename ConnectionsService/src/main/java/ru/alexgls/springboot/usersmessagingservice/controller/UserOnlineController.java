package ru.alexgls.springboot.usersmessagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.alexgls.springboot.usersmessagingservice.dto.CheckOnlineRequest;
import ru.alexgls.springboot.usersmessagingservice.service.PresenceService;

import java.util.Map;

@RestController
@RequestMapping("/api/online")
@RequiredArgsConstructor
@Slf4j
public class UserOnlineController {

    private final PresenceService presenceService;

    @PostMapping("/check-by-list")
    public Map<Integer, Boolean>checkUsersOnlineByList(@RequestBody CheckOnlineRequest checkOnlineRequest) {
        log.info("Check online request: {}", checkOnlineRequest);
        return presenceService.checkOnlineByList(checkOnlineRequest.usersIds());
    }

}
