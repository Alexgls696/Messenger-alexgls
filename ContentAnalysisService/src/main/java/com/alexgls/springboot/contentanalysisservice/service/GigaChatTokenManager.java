package com.alexgls.springboot.contentanalysisservice.service;

import com.alexgls.springboot.contentanalysisservice.client.ContentAnalysisOauthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class GigaChatTokenManager {

    private final ContentAnalysisOauthClient oauthClient;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();

    public String getToken() {
        String token = cachedToken.get();
        if (token == null) {
            return refreshToken();
        }
        return token;
    }

    public synchronized String refreshToken() {
        try {
            var response = oauthClient.getOauthTokenRequest();
            String newToken = response.access_token();
            cachedToken.set(newToken);
            return newToken;
        } catch (Exception e) {

            throw new RuntimeException("CRITICAL: Не удалось обновить токен", e);
        }
    }

    // Метод для инвалидации (сброса) токена при ошибке 401
    public void invalidateToken(String badToken) {
        cachedToken.compareAndSet(badToken, null);
    }
}