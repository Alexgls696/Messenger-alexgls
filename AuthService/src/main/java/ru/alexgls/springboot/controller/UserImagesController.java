package ru.alexgls.springboot.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.alexgls.springboot.dto.user_details.AddProfileImageRequest;
import ru.alexgls.springboot.service.UserProfileService;

@RestController
@RequestMapping("/api/profiles/images")
@RequiredArgsConstructor
@Slf4j
public class UserImagesController {

    private final UserProfileService userProfileService;

    @PostMapping
    public void addImageToUserProfile(@RequestBody AddProfileImageRequest addProfileImageRequest, Authentication authentication) {
        int userId = getCurrentUserId(authentication);
        log.info("Add profile image to user with id : {}", userId);
        userProfileService.addImageToUserProfile(addProfileImageRequest.imageId(), userId);
    }

    @GetMapping("/user-avatar")
    public int findUserAvatarImageId(Authentication authentication) {
        int userId = getCurrentUserId(authentication);
        return userProfileService.findUserAvatarImageId(userId);
    }

    @GetMapping("/user-avatar/{id}")
    public int findUserAvatarImageId(@PathVariable("id") int id) {
        return userProfileService.findUserAvatarImageId(id);
    }

    @DeleteMapping("/{userImageId}")
    public void deleteImageFromUserProfileById(@PathVariable("userImageId") int id, Authentication authentication) {
        int userId = getCurrentUserId(authentication);
        log.info("Delete profile image from user with id : {}", userId);
        userProfileService.deleteImageFromUserProfile(id, userId);
    }

    private Integer getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }

}
