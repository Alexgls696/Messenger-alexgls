package ru.alexgls.springboot.usersmessagingservice.client;


import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import ru.alexgls.springboot.usersmessagingservice.dto.JwtValidationRequest;
import ru.alexgls.springboot.usersmessagingservice.dto.JwtValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import ru.alexgls.springboot.usersmessagingservice.exception.ServiceUnauthorizedException;


@RequiredArgsConstructor
public class AuthServiceClientImpl implements AuthServiceClient {

    private final RestClient restClient;

    @Override
    public JwtValidationResponse validateToken(String token) {
        try {
            return restClient
                    .post()
                    .uri("/auth/validate")
                    .body(new JwtValidationRequest(token))
                    .retrieve()
                    .body(JwtValidationResponse.class);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ServiceUnauthorizedException("Токен доступа недействителен");
        }
    }

    @Override
    public String findUserInitialsById(int id, String token) {
        try {
            return restClient
                    .get()
                    .uri("/users/{id}", id)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ServiceUnauthorizedException("Токен доступа недействителен");
        }
    }
}
