package ru.alexgls.springboot.config;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.alexgls.springboot.dto.JwtValidationResponse;

import java.security.interfaces.RSAPublicKey;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    @Value("${jwt.expiration.users}")
    private Long expiration;

    @Value("${jwt.expiration.service}")
    private Long serviceTokenExpiration;

    private final KeyPairProvider keyProvider;

    public Claims getAllClaimsFromToken(String token) {
        RSAPublicKey publicKey = keyProvider.getPublicKey();
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public String generateToken(String username, Integer userId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("userId", String.valueOf(userId));
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + expiration;
        Date exp = new Date(expMillis);
        return Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, keyProvider.getKeyId())
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setIssuer("http://localhost:8085")
                .setExpiration(exp)
                .signWith(keyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateTokenForService(String serviceClientId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + serviceTokenExpiration;
        Date exp = new Date(expMillis);

        return Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, keyProvider.getKeyId())
                .setClaims(claims)
                .setSubject(serviceClientId)
                .setIssuedAt(now)
                .setIssuer("http://localhost:8085")
                .setExpiration(exp)
                .signWith(keyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public JwtValidationResponse validateTokenAndGetJwtValidationResponse(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return JwtValidationResponse.builder() // или через конструктор
                    .valid(true)
                    .userId(claims.get("userId", String.class))
                    .roles((List<String>) claims.get("roles"))
                    .build();
        } catch (ExpiredJwtException e) {
            log.warn("JWT истек: {}", e.getMessage());
            return JwtValidationResponse.builder()
                    .valid(false)
                    .message("EXPIRED") // Помечаем специально для сервиса соединений
                    .build();
        } catch (Exception e) {
            log.error("Ошибка валидации JWT: {}", e.getMessage());
            return JwtValidationResponse.builder()
                    .valid(false)
                    .message("INVALID")
                    .build();
        }
    }

}
