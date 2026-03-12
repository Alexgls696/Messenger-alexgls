package com.alexgls.springboot.messagestorageservicevt.client;

import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUserException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.ServiceUnauthorizedException;
import com.github.dockerjava.api.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@RequiredArgsConstructor
public class AuthRestClientImpl implements AuthRestClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<List<GetUserDto>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };

    @Override

    public GetUserDto findUserById(int id, String token) {
        try {
            return restClient
                    .get()
                    .uri("api/users/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token))
                    .retrieve()
                    .body(GetUserDto.class);
        } catch (HttpClientErrorException.NotFound notFound) {
            throw new NoSuchUserException("Пользователь с  %d не найден".formatted(id));
        } catch (HttpClientErrorException.Unauthorized unauthorized) {
            throw new ServiceUnauthorizedException("Срок действия токена доступа истек. Повторите попытку.");
        } catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), exception.getResponseBodyAsString());
        }
    }

    @Override
    public List<GetUserDto> findAllUsers(Iterable<Integer> ids, String token) {
        try {
            return restClient.post()
                    .uri("api/users/find-by-ids")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token))
                    .body(ids)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        } catch (HttpClientErrorException.Unauthorized unauthorized) {
            throw new ServiceUnauthorizedException("Срок действия токена доступа истек. Повторите попытку.");
        }
    }
}
