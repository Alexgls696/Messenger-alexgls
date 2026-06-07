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
import java.util.Set;

@RestController
@RequestMapping("/auth/services")
@RequiredArgsConstructor
@Slf4j
public class ServicesTokenController {

    private final JwtUtil jwtUtil;

    @Value("${services.ids}")
    private Set<String> registerClientIds;

    @Value("${services.secrets}")
    private Set<String> registerClientSecrets;

    @Value("${jwt.expiration.service}")
    private Long serviceTokenExpiration;

    public record ServiceLoginRequest(String clientId, String clientSecret) {
    }

    @PostMapping("/token")
    public ResponseEntity<?> getServiceToken(@RequestBody ServiceLoginRequest request) {
        log.info("Received token request: {}", request);
        if (registerClientIds.contains(request.clientId) && registerClientSecrets.contains(request.clientSecret)) {
            String serviceToken = jwtUtil.generateTokenForService(
                    request.clientId(),
                    List.of("ROLE_SERVICE")
            );

            return ResponseEntity.ok(Map.of(
                    "accessToken", serviceToken,
                    "tokenType", "Bearer",
                    "expiresIn", serviceTokenExpiration / 1000
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "invalid_client", "error_description", "Invalid client credentials"));
    }

}
