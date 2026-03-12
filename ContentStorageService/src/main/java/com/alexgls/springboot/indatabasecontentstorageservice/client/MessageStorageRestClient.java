package com.alexgls.springboot.indatabasecontentstorageservice.client;

import com.alexgls.springboot.indatabasecontentstorageservice.exception.MessageStorageServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class MessageStorageRestClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<Iterable<Integer>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };

    public Iterable<Integer> getParticipantsByChatId(long chatId) {
        try {
            return restClient
                    .get()
                    .uri("/api/chats/{id}/participants-ids", chatId)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (HttpClientErrorException exception) {
            throw new MessageStorageServiceException("Не удалось получить данные из сервиса сообщений");
        }
    }

}
