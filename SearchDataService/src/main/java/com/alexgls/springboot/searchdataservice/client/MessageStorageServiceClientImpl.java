package com.alexgls.springboot.searchdataservice.client;

import com.alexgls.springboot.searchdataservice.dto.MessageDto;
import com.alexgls.springboot.searchdataservice.dto.SearchMessageInChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@RequiredArgsConstructor
public class MessageStorageServiceClientImpl implements MessageStorageServiceClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<List<MessageDto>> messageTypeReference = new ParameterizedTypeReference<>() {};

    @Override
    public List<MessageDto> findMessagesByContent(SearchMessageInChatRequest searchMessageInChatRequest, String token) {
        try {
            return restClient.post()
                    .uri("/api/messages/find-by-content-in-chat")
                    .header("Authorization", "Bearer " + token)
                    .body(searchMessageInChatRequest)
                    .retrieve()
                    .body(messageTypeReference);
        } catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), "Ошибка при обращении к сервису сообщений: " + exception.getResponseBodyAsString());
        }
    }
}
