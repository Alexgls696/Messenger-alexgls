package ru.alexgls.springboot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.alexgls.springboot.config.JwtUtil;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/services")
@RequiredArgsConstructor
@Slf4j
public class ServicesTokenController {

    private final JwtUtil jwtUtil;

    @Value("${services.register.id}")
    private String registerClientId;

    @Value("${services.register.secret}")
    private String registerClientSecret;

    @Value("${jwt.expiration.service}")
    private Long serviceTokenExpiration;



    public record ServiceLoginRequest(String clientId, String clientSecret) {}

    @PostMapping("/token")
    public ResponseEntity<?> getServiceToken(@RequestBody ServiceLoginRequest request) {
        if (registerClientId.equals(request.clientId()) && registerClientSecret.equals(request.clientSecret())) {
            String serviceToken = jwtUtil.generateTokenForService(
                    request.clientId(),
                    List.of("ROLE_SERVICE")
            );

            return ResponseEntity.ok(Map.of(
                    "access_token", serviceToken,
                    "token_type", "Bearer",
                    "expires_in", serviceTokenExpiration / 1000
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "invalid_client", "error_description", "Invalid client credentials"));
    }

}
