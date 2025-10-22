package com.alexgls.springboot.userdetailsservice.controller;


import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.dto.UserProfileResponse;
import com.alexgls.springboot.userdetailsservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    //Добавить создание профиля при создании аккаунта.
    @PostMapping("/create")
    public Mono<Void> createProfileByUserId(Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        log.info("Create profile for user with id: {}", userId);
        return userProfileService.createProfileForUserByUserId(userId);
    }

    @GetMapping("/{id}")
    public Mono<UserProfileResponse> findProfileByUserId(@PathVariable("id") int userId) {
        log.info("Find profile for user with id: {}", userId);
        return userProfileService.findUserProfileByUserId(userId);
    }

    @PostMapping("/update")
    public Mono<Void> updateProfileByUserId(@RequestBody UpdateUserDetailsRequest updateUserDetailsRequest, Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        log.info("Update profile for user with id: {}", userId);
        return userProfileService.updateUserDetails(updateUserDetailsRequest, userId);
    }

    private Integer getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }
}
