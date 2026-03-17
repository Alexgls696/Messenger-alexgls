package ru.alexgls.springboot.usersmessagingservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import ru.alexgls.springboot.usersmessagingservice.exception.MessageStorageServiceException;

@RequiredArgsConstructor
public class MessagesStorageServiceRestClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<Iterable<Integer>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

    public Iterable<Integer> findAllUsersWhoHadChatWithUser(int userId) {
        try {
            return restClient
                    .get()
                    .uri("/api/chats/search-users/{id}", userId)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (Exception exception) {
            throw new MessageStorageServiceException("При обращении к сервису хранения сообщений произошла ошибка: " + exception.getMessage());
        }
    }

}
