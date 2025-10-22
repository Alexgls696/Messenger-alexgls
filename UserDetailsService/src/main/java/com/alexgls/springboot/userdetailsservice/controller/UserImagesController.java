package com.alexgls.springboot.userdetailsservice.controller;

import com.alexgls.springboot.userdetailsservice.dto.AddProfileImageRequest;
import com.alexgls.springboot.userdetailsservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/profiles/images")
@RequiredArgsConstructor
@Slf4j
public class UserImagesController {

    private final UserProfileService userProfileService;

    @PostMapping
    public Mono<Void> addImageToUserProfile(@RequestBody AddProfileImageRequest addProfileImageRequest, Authentication authentication) {
        int userId = getCurrentUserId(authentication);
        log.info("Add profile image to user with id : {}", userId);
        return userProfileService.addImageToUserProfile(addProfileImageRequest.imageId(), userId);
    }

    @GetMapping("/user-avatar")
    public Mono<Integer>findUserAvatarImageId(Authentication authentication) {
        int userId = getCurrentUserId(authentication);
        return userProfileService.findUserAvatarImageId(userId);
    }

    @GetMapping("/user-avatar/{id}")
    public Mono<Integer>findUserAvatarImageId(@PathVariable("id") int id) {
        return userProfileService.findUserAvatarImageId(id);
    }

    @DeleteMapping("/{userImageId}")
    public Mono<Void> deleteImageFromUserProfileById(@PathVariable("userImageId") int id, Authentication authentication) {
        int userId = getCurrentUserId(authentication);
        log.info("Delete profile image from user with id : {}", userId);
        return userProfileService.deleteImageFromUserProfile(id, userId);
    }

    private Integer getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }

}
