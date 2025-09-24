package ru.alexgls.springboot.controller;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Get;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.config.JwtUtil;
import ru.alexgls.springboot.dto.*;
import ru.alexgls.springboot.entity.RefreshToken;
import ru.alexgls.springboot.entity.User;
import ru.alexgls.springboot.exceptions.RefreshTokenNotFoundException;
import ru.alexgls.springboot.service.RefreshTokenService;
import ru.alexgls.springboot.service.UsersService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Mono<ResponseEntity<?>> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login request: username={}", loginRequest.username);
        Mono<Boolean> checkCredentialsResultMono = usersService.checkCredentials(loginRequest.username, loginRequest.password);
        return checkCredentialsResultMono
                .flatMap(result -> {
                    if (result) {
                        return getLoginResponseByUsername(loginRequest.username);
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Неверное имя пользователя или пароль")));
                    }
                });
    }

    @Transactional
    @PostMapping("/refresh")
    public Mono<ResponseEntity<?>> refreshToken(@RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        return refreshTokenService.findByToken(requestRefreshToken)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RefreshTokenNotFoundException("Refresh token не найден в базе данных."))))
                .flatMap(refreshTokenService::verifyExpiration) // verifiedToken - это старый токен
                .flatMap(verifiedToken -> {
                    Mono<RefreshToken> newRefreshTokenMono = refreshTokenService.createRefreshToken(verifiedToken.getUserId());
                    Mono<Void> deleteOldTokenMono = refreshTokenService.deleteByToken(verifiedToken.getToken());
                    return deleteOldTokenMono.then(newRefreshTokenMono);
                })
                .flatMap(newRefreshToken -> usersService.findUserById(newRefreshToken.getUserId())
                        .flatMap(user -> usersService.getUserRoles(user.getId()).collectList()
                                .map(roles -> {
                                    String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getId(), roles);
                                    return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken.getToken()));
                                })
                        )
                );
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@RequestBody UserRegisterDto userRegisterDto) {
        log.info("Register user: username={} email={}", userRegisterDto.username(), userRegisterDto.email());
        return usersService.saveUser(userRegisterDto)
                .flatMap(dto -> getLoginResponseByUsername(dto.username()));
    }


    @PostMapping("/validate")
    public ResponseEntity<JwtValidationResponse> validateJwtToken(@RequestBody JwtValidationRequest tokenRequest) {
        log.info("Try to validate token: {}", tokenRequest);
        return ResponseEntity
                .ok(jwtUtil.validateTokenAndGetJwtValidationResponse(tokenRequest.getToken()));
    }

    private Mono<ResponseEntity<JwtResponse>> getLoginResponseByUsername(String username) {
        Mono<User> userMono = usersService.getUserByUsername(username);
        Mono<List<String>> rolesMono = userMono
                .flatMap(user -> usersService
                        .getUserRoles(user.getId())
                        .collect(Collectors.toList()));
        return Mono.zip(userMono, rolesMono)
                .flatMap(tuple -> {
                    var user = tuple.getT1();
                    List<String> roles = tuple.getT2();
                    String accessToken = jwtUtil.generateToken(username, user.getId(), roles);
                    return refreshTokenService.createRefreshToken(user.getId())
                            .map(refreshToken -> {
                                JwtResponse jwtResponse = new JwtResponse(accessToken, refreshToken.getToken());
                                return ResponseEntity.ok(jwtResponse);
                            });
                });
    }
}