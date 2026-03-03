package ru.alexgls.springboot.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.util.UUID;

@Component
@Getter
public class KeyPairProvider {

    private final KeyPair keyPair;
    private final String keyId;

    public KeyPairProvider() {

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            this.keyPair = keyPairGenerator.generateKeyPair();

            this.keyId = UUID.randomUUID().toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось сгенерировать RSA пару ключей", ex);
        }
    }

    public RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }

    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }
}
