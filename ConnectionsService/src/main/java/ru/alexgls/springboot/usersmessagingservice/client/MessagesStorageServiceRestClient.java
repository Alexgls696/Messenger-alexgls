package ru.alexgls.springboot.usersmessagingservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import ru.alexgls.springboot.usersmessagingservice.exception.MessageStorageServiceException;

@RequiredArgsConstructor
public class MessagesStorageServiceRestClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<Iterable<Integer>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

    public Iterable<Integer> findAllUsersWhoHadChatWithUser(String token) {
        try {
            return restClient
                    .get()
                    .uri("/api/chats/search-users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (Exception exception) {
            throw new MessageStorageServiceException("При обращении к сервису хранения сообщений произошла ошибка: " + exception.getMessage());
        }
    }

}
