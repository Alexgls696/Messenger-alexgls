package com.alexgls.springboot.searchdataservice.client;

import com.alexgls.springboot.searchdataservice.dto.GetUserDto;
import com.alexgls.springboot.searchdataservice.exception_handling.ServiceUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@RequiredArgsConstructor
public class AuthServiceRestClientImpl implements AuthServiceRestClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<Iterable<GetUserDto>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

    @Override
    public Iterable<GetUserDto> findAllByUsername(String username, String token) {
        try {
            return restClient
                    .get()
                    .uri("/api/users/find-all-by-username/{username}", username)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), "Ошибка при обращении к сервису пользователей: " + exception.getResponseBodyAsString());
        }
    }

    @Override
    public Iterable<GetUserDto> findAllByIds(List<Integer> ids, String token) {
        try {
            return restClient
                    .post()
                    .uri("/api/users/find-by-ids")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(ids)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ServiceUnauthorizedException("Токен доступа недействительный, повторите попытку");
        }
        catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), "Ошибка при обращении к сервису пользователей: " + exception.getResponseBodyAsString());
        }
    }
}
