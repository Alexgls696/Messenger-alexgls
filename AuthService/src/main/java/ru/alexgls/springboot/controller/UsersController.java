package ru.alexgls.springboot.controller;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.alexgls.springboot.dto.ExistsUserRequest;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.dto.UpdateUserRequest;
import ru.alexgls.springboot.dto.UserExistsResponse;
import ru.alexgls.springboot.exceptions.ExistsUserRequestException;
import ru.alexgls.springboot.exceptions.InvalidJwtException;
import ru.alexgls.springboot.exceptions.NoSuchAuthException;
import ru.alexgls.springboot.service.UsersService;
import ru.alexgls.springboot.utils.AuthUtil;

import java.util.Objects;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Slf4j
public class UsersController {

    private final UsersService usersService;

    @GetMapping("initials/{id}")
    public String findUserInitialsById(@PathVariable("id") int id) {
        log.info("Find user initials by id {}", id);
        return usersService.findUserInitialsById(id);
    }

    @GetMapping
    public Iterable<GetUserDto> getUsers() {
        log.info("Get users");
        return usersService.findAllUsers();
    }

    @GetMapping("/{id}")
    public GetUserDto findUserById(@PathVariable("id") int id) {
        log.info("Find user by id {}", id);
        return usersService.findUserDtoById(id);
    }

    @GetMapping("/me")
    public GetUserDto getCurrentUser(Authentication authentication) {
        log.info("Try to get current user");
        if (Objects.isNull(authentication) || Objects.isNull(authentication.getPrincipal())) {
            throw new NoSuchAuthException("User is not authenticated");
        }
        int userId = getUserId(authentication);
        return usersService.findUserDtoById(userId);
    }

    private int getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        if (jwt.getClaims().containsKey("userId")) {
            int userId = Integer.parseInt(jwt.getClaim("userId").toString());
            log.info("Get current user with id {}", userId);
            return userId;
        } else {
            throw new InvalidJwtException("Jwt is invalid: claim 'userId' is missing");
        }
    }


    @PostMapping("/update-password")
    public ResponseEntity<Void> setPasswordForCurrentUser(@Size(min = 8) @RequestBody String password, Authentication authentication) {
        int userId = getUserId(authentication);
        log.info("Set password for userId {}", userId);
        usersService.setPasswordForUserById(userId, password);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/update")
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserRequest updateUserRequest, Authentication authentication) {
        log.info("Update user {}", updateUserRequest);
        int currentUserId = AuthUtil.getCurrentUserId(authentication);
        usersService.updateUserInfo(updateUserRequest, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/exists")
    public UserExistsResponse existsByUsernameOrEmail(@RequestBody ExistsUserRequest existsUserRequest) {
        try {
            log.info("Check for exists user with username {} and email {}", existsUserRequest.username(), existsUserRequest.email());
            boolean exists = usersService.existsByUsernameOrEmail(existsUserRequest.username(), existsUserRequest.email());
            return new UserExistsResponse(exists);
        } catch (Exception exception) {
            throw new ExistsUserRequestException(exception.getMessage());
        }
    }
}