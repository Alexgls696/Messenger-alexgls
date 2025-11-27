package com.alexgls.springboot.contentanalysisservice.service;

import com.alexgls.springboot.contentanalysisservice.client.ContentAnalysisOauthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class GigaChatTokenManager {

    private final ContentAnalysisOauthClient oauthClient;

    // AtomicReference гарантирует атомарность чтения/записи
    private final AtomicReference<String> cachedToken = new AtomicReference<>();

    public String getToken() {
        String token = cachedToken.get();
        if (token == null) {
            return refreshToken();
        }
        return token;
    }

    /**
     * Обновляет токен. Метод synchronized, чтобы 10 потоков
     * не долбили API авторизации одновременно.
     */
    public synchronized String refreshToken() {
        // Double-check locking: пока ждали лок, токен мог уже обновиться другим потоком
        // (можно добавить проверку, если хотите идеальной оптимизации, но для начала так сойдет)

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
        // Сбрасываем только если в кеше всё еще лежит этот "плохой" токен
        cachedToken.compareAndSet(badToken, null);
    }
}