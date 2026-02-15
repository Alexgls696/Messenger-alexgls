package com.alexgls.springboot.searchdataservice.client;

import com.alexgls.springboot.searchdataservice.dto.MessageDto;
import com.alexgls.springboot.searchdataservice.dto.SearchMessageInChatRequest;
import com.alexgls.springboot.searchdataservice.exception_handling.ServiceUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@RequiredArgsConstructor
public class MessageStorageServiceClientImpl implements MessageStorageServiceClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<List<MessageDto>> messageTypeReference = new ParameterizedTypeReference<>() {
    };

    private final ParameterizedTypeReference<List<Integer>> userIds = new ParameterizedTypeReference<>() {
    };

    @Override
    public List<MessageDto> findMessagesByContent(SearchMessageInChatRequest searchMessageInChatRequest, String token) {
        try {
            return restClient.post()
                    .uri("/api/messages/find-by-content-in-chat")
                    .header("Authorization", "Bearer " + token)
                    .body(searchMessageInChatRequest)
                    .retrieve()
                    .body(messageTypeReference);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ServiceUnauthorizedException("Токен доступа недействительный, повторите попытку");
        } catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), "Ошибка при обращении к сервису сообщений: " + exception.getResponseBodyAsString());
        }
    }

    @Override
    public List<Integer> findAllUsersWhoHadChatWith(String token) {
        try {
            return restClient.get()
                    .uri("/api/chats/search-users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(userIds);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ServiceUnauthorizedException("Токен доступа недействительный, повторите попытку");
        } catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), "Ошибка при обращении к сервису сообщений: " + exception.getResponseBodyAsString());
        }
    }
}
