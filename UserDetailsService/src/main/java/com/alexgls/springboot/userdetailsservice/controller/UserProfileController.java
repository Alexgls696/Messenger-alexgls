package com.alexgls.springboot.userdetailsservice.controller;

import com.alexgls.springboot.userdetailsservice.dto.UserProfileResponse;
import com.alexgls.springboot.userdetailsservice.service.UserProfileService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/create")
    public Mono<UserProfileResponse> createProfileByUserId(@PathVariable("id") int userId) {
        log.info("Create profile for user with id: {}", userId);
        return userProfileService.createProfileForUserByUserId(userId);
    }

    @GetMapping("/{id}")
    public Mono<UserProfileResponse> findProfileByUserId(@PathVariable("id") int userId) {
        log.info("Find profile for user with id: {}", userId);
        return userProfileService.findUserProfileByUserId(userId);
    }
}
