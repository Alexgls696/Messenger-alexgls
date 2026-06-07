package com.alexgls.springboot.notificationsservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class MessageStorageServiceClientImpl implements MessageStorageServiceClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<List<Integer>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };

    @Override
    public List<Integer> findAllParticipantsByChatId(int chatId, String token) {
        try {
            return restClient
                    .get()
                    .uri("/api/chats/{chatId}/participants-ids", chatId)
                    .header("Authorization", "Bearer %s".formatted(token))
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (HttpClientErrorException exception) {
            log.error("При обращении к сервису сообщений произошла ошибка: {}", exception.getResponseBodyAsString());
            exception.printStackTrace();
            throw exception;
        }
    }
}
