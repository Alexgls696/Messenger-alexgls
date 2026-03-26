package com.alexgls.springboot.indatabasecontentstorageservice.client;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.UserParticipantDto;
import com.alexgls.springboot.indatabasecontentstorageservice.exception.MessageStorageServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
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

    public UserParticipantDto existsByChatIdAndUserId(int chatId, int userId, String token) {
        try {
            return restClient.get()
                    .uri("/api/chats/{chatId}/participants/exists/{userId}", chatId, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(UserParticipantDto.class);
        } catch (HttpClientErrorException exception) {
            throw new MessageStorageServiceException("Не удалось получить данные из сервиса сообщений");
        }
    }
}
