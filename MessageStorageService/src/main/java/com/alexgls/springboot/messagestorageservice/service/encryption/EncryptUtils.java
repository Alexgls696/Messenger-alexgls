package com.alexgls.springboot.messagestorageservice.service.encryption;


import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class EncryptUtils {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // Рекомендованный размер IV для GCM - 96 бит
    private static final int GCM_TAG_LENGTH_BITS = 128; // Размер аутентификационного тега

    private final SecretKeySpec aesKeySpec;
    private final String hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptUtils(@Value("${app.security.aes-key}") String aesKeyHex,
                        @Value("${app.security.hmac-key}") String hmacKey) {

        Objects.requireNonNull(aesKeyHex, "AES key cannot be null");
        Objects.requireNonNull(hmacKey, "HMAC key cannot be null");

        if (aesKeyHex.length() != 32) {
            throw new IllegalArgumentException("Invalid AES key length. Must be 32 hex characters (16 bytes).");
        }

        byte[] aesKeyBytes = hexStringToByteArray(aesKeyHex);
        this.aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
        this.hmacKey = hmacKey;
    }

    public String calculateHmac(String word) {
        if (word == null) {
            return null;
        }
        return new HmacUtils(HMAC_ALGO, hmacKey).hmacHex(word);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, gcmParameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] ivAndCiphertext = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
            System.arraycopy(ciphertext, 0, ivAndCiphertext, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(ivAndCiphertext);

        } catch (Exception e) {
            throw new RuntimeException("Error during encryption", e);
        }
    }

    public String decrypt(String base64Ciphertext) {
        if (base64Ciphertext == null || base64Ciphertext.isEmpty()) {
            return base64Ciphertext;
        }

        try {
            byte[] ivAndCiphertext = Base64.getDecoder().decode(base64Ciphertext);
            if (ivAndCiphertext.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted data: shorter than IV length.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(ivAndCiphertext, 0, iv, 0, iv.length);

            byte[] ciphertext = new byte[ivAndCiphertext.length - iv.length];
            System.arraycopy(ivAndCiphertext, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKeySpec, gcmParameterSpec);

            byte[] plaintextBytes = cipher.doFinal(ciphertext);

            return new String(plaintextBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Error during decryption. The data might be corrupted or the key is wrong.", e);
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}