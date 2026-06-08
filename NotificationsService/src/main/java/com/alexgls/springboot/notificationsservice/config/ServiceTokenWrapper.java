package com.alexgls.springboot.notificationsservice.config;

import com.alexgls.springboot.notificationsservice.dto.ServiceLoginResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public class ServiceTokenWrapper {

    // Неизменяемый объект для хранения состояния токена
    private record TokenState(String token, long expiresAtMillis) {}

    // AtomicReference гарантирует атомарность чтения и записи ссылки
    private final AtomicReference<TokenState> tokenRef = new AtomicReference<>(new TokenState(null, 0L));

    // Буфер времени (в мс), чтобы начать обновление токена чуть раньше его реального истечения
    private static final long REFRESH_BUFFER_MILLIS = 60_000; // 1 минута

    /**
     * Получает текущий валидный токен. Если токен истек или отсутствует, 
     * инициирует его обновление.
     *
     * @param refreshSupplier логика получения нового токена (например, HTTP-запрос)
     * @return валидный токен
     */
    public String getValidToken(Supplier<ServiceLoginResponse> refreshSupplier) {
        TokenState currentState = tokenRef.get();
        long now = System.currentTimeMillis();

        // 1. Быстрая проверка: если токен валиден, возвращаем его сразу (без блокировок)
        if (currentState.token() != null && currentState.expiresAtMillis() > now + REFRESH_BUFFER_MILLIS) {
            return currentState.token();
        }

        // 2. Если токен истек или отсутствует, блокируем только для обновления
        // synchronized предотвращает одновременные сетевые запросы на обновление от разных потоков
        synchronized (this) {
            // 3. Double-Checked Locking: проверяем состояние еще раз внутри блокировки.
            // Возможно, другой поток уже обновил токен, пока мы ждали входа в synchronized.
            currentState = tokenRef.get();
            if (currentState.token() != null && currentState.expiresAtMillis() > now + REFRESH_BUFFER_MILLIS) {
                return currentState.token();
            }

            ServiceLoginResponse serviceLoginResponse = refreshSupplier.get();
            // 4. Выполняем реальное обновление токена (сетевой вызов)
            String newToken = serviceLoginResponse.accessToken();
            
            // TODO: Здесь должна быть логика парсинга времени истечения из newToken 
            // (например, декодирование JWT или чтение поля expires_in из ответа OAuth)
            long newExpiresAt = serviceLoginResponse.expiresIn();

            // 5. Атомарно обновляем состояние для всех остальных потоков
            tokenRef.set(new TokenState(newToken, newExpiresAt));
            
            return newToken;
        }
    }

    /**
     * Принудительная очистка токена (например, при явном выходе из системы или ошибке 401).
     */
    public void invalidate() {
        tokenRef.set(new TokenState(null, 0L));
    }

    // Заглушка для примера. В реальности здесь парсится JWT или берется из ответа API.
    private long calculateExpirationTime(String token) {
        return System.currentTimeMillis() + 3600_000; // +1 час для примера
    }
}