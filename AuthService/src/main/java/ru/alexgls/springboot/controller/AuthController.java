package ru.alexgls.springboot.controller;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.alexgls.springboot.config.JwtUtil;
import ru.alexgls.springboot.dto.*;
import ru.alexgls.springboot.entity.RefreshToken;
import ru.alexgls.springboot.entity.User;
import ru.alexgls.springboot.exceptions.RefreshTokenNotFoundException;
import ru.alexgls.springboot.service.RefreshTokenService;
import ru.alexgls.springboot.service.UsersService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UsersService usersService;
    private final RefreshTokenService refreshTokenService;

    public record LoginRequest(String username, String password) {
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class JwtResponse {
        private String accessToken;
        private String refreshToken;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login request: username={}", loginRequest.username);
        boolean credentialsValid = usersService.checkCredentials(loginRequest.username, loginRequest.password);
        if (credentialsValid) {
            return getLoginResponseByUsername(loginRequest.username);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверное имя пользователя или пароль"));
        }
    }

    @PreAuthorize("hasRole('SERVICE')")
    @PostMapping("/login-by-email")
    public ResponseEntity<JwtResponse> loginById(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        log.info("Login request for user with email: userId={}", email);
        GetUserDto userDto = usersService.findUserByEmail(email);
        return getLoginResponseByUsername(userDto.username());
    }

    @Transactional
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken verifiedToken = refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token не найден или его срок действия истек."));

        refreshTokenService.deleteByToken(verifiedToken.getToken());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(verifiedToken.getUserId());

        User user = usersService.findUserById(newRefreshToken.getUserId());
        List<String> roles = usersService.getUserRoles(user.getId());

        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getId(), roles);

        return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken.getToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@RequestBody UserRegisterDto userRegisterDto) {
        log.info("Register user: username={} email={}", userRegisterDto.username(), userRegisterDto.email());
        GetUserDto dto = usersService.saveUser(userRegisterDto);
        return getLoginResponseByUsername(dto.username());
    }

    @PostMapping("/validate")
    public ResponseEntity<JwtValidationResponse> validateJwtToken(@RequestBody JwtValidationRequest tokenRequest) {
        log.info("Try to validate token: {}", tokenRequest);
        return ResponseEntity
                .ok(jwtUtil.validateTokenAndGetJwtValidationResponse(tokenRequest.getToken()));
    }

    private ResponseEntity<JwtResponse> getLoginResponseByUsername(String username) {
        User user = usersService.getUserByUsername(username);
        List<String> roles = usersService.getUserRoles(user.getId());
        String accessToken = jwtUtil.generateToken(username, user.getId(), roles);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        JwtResponse jwtResponse = new JwtResponse(accessToken, refreshToken.getToken());
        return ResponseEntity.ok(jwtResponse);
    }
}