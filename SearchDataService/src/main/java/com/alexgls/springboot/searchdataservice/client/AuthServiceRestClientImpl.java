package com.alexgls.springboot.searchdataservice.client;

import com.alexgls.springboot.searchdataservice.dto.GetUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class AuthServiceRestClientImpl implements AuthServiceRestClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<Iterable<GetUserDto>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };

    @Override
    public Iterable<GetUserDto> findAllByUsername(String username, String token) {
        try {
            return restClient
                    .get()
                    .uri("/api/users/find-all-by-username/{username}", username)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), "Ошибка при обращении к сервису пользователей: " + exception.getResponseBodyAsString());
        }
    }
}
