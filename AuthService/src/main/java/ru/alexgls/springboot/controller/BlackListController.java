package ru.alexgls.springboot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.dto.blacklist.*;
import ru.alexgls.springboot.service.UsersService;
import ru.alexgls.springboot.utils.AuthUtil;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/black-list")
@Slf4j
@RequiredArgsConstructor
public class BlackListController {

    private final UsersService usersService;

    @PostMapping("/black-list")
    public ResponseEntity<AddUserToBlackListResponse> addUserToBlackList(Authentication authentication, @RequestParam int targetUserId) {
        int userId = AuthUtil.getCurrentUserId(authentication);
        log.info("Block user request: target_id: {}, current_id: {}", targetUserId, userId);
        var responseBody = usersService.addUserToBlackList(userId, targetUserId);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/black-list/delete")
    public ResponseEntity<DeleteUserFromBlackListResponse> deleteUserFromBlackList(Authentication authentication, @RequestParam int targetUserId) {
        int userId = AuthUtil.getCurrentUserId(authentication);
        log.info("Unblock user request: target_id: {}, current_id: {}", targetUserId, userId);
        var responseBody = usersService.deleteUserFromBlackList(userId,targetUserId);
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping
    public List<GetUserDto> findAllBlockedUsersByUserId(Authentication authentication) {
        log.info("Find all blocked users by userId: {}", authentication);
        int userId = AuthUtil.getCurrentUserId(authentication);
        return usersService.getBlockedUsersListByUserId(userId);
    }

    @PostMapping("/is_blocked")
    public ResponseEntity<Map<String, Object>> isBlocked(@RequestParam int targetUserId, Authentication authentication) {
        int userId = AuthUtil.getCurrentUserId(authentication);
        log.info("Is blocked user request: target_id: {}, current_id: {}", targetUserId, userId);
        boolean isBlocked = usersService.isBlocked(userId, targetUserId);
        return ResponseEntity
                .ok(Map.of("isBlocked", isBlocked));
    }

}
