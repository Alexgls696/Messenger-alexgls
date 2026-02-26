package ru.alexgls.springboot.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.alexgls.springboot.dto.user_details.UpdateUserDetailsRequest;
import ru.alexgls.springboot.dto.user_details.UserProfileResponse;
import ru.alexgls.springboot.service.UserProfileService;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/create")
    public void createProfileByUserId(Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        log.info("Create profile for user with id: {}", userId);
        userProfileService.createProfileForUserByUserId(userId);
    }

    @GetMapping("/{id}")
    public UserProfileResponse findProfileByUserId(@PathVariable("id") int userId) {
        log.info("Find profile for user with id: {}", userId);
        return userProfileService.findUserProfileByUserId(userId);
    }

    @PostMapping("/update")
    public void updateProfileByUserId(@RequestBody UpdateUserDetailsRequest updateUserDetailsRequest, Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        log.info("Update profile for user with id: {}", userId);
        userProfileService.updateUserDetails(updateUserDetailsRequest, userId);
    }

    private Integer getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }
}
