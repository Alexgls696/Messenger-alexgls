package ru.alexgls.springboot.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.alexgls.springboot.config.KeyPairProvider;

import java.util.Date;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwkSetController {
    private final KeyPairProvider keyPairProvider;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey rsaKey = new RSAKey.Builder(keyPairProvider.getPublicKey())
                .keyID(keyPairProvider.getKeyId())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return jwkSet.toJSONObject();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP" ,
                "server-time", new Date());
    }

}
