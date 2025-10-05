package com.alexgls.springboot.registrationservice.client;

import com.alexgls.springboot.registrationservice.dto.AuthServiceExistsUserRequest;
import com.alexgls.springboot.registrationservice.dto.AuthServiceJwtResponse;
import com.alexgls.springboot.registrationservice.dto.AuthServiceUserExistsResponse;
import com.alexgls.springboot.registrationservice.dto.UserRegisterDto;
import com.alexgls.springboot.registrationservice.exception.AccessToAuthServiceException;
import com.alexgls.springboot.registrationservice.exception.AuthServiceException;
import com.alexgls.springboot.registrationservice.exception.UserNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AuthServiceClientImpl implements AuthServiceClient {
    private final RestClient restClient;

    @Value("${service.client-id}")
    private String clientId;

    @Value("${service.client-secret}")
    private String clientSecret;

    public static String correctJwtToken = "";

    @Override
    public AuthServiceJwtResponse registerUser(UserRegisterDto userRegisterDto) {
        try {
            return restClient
                    .post()
                    .uri("/auth/register")
                    .body(userRegisterDto)
                    .retrieve()
                    .body(AuthServiceJwtResponse.class);
        } catch (HttpClientErrorException exception) {
            throw new AuthServiceException("При обращение к AuthService возникла ошибка: " + exception.getMessage());
        }
    }

    @Override
    public AuthServiceJwtResponse loginUserByEmail(String email) {
        try {
            return tryToLoginUserByEmail(email);
        } catch (AccessToAuthServiceException accessException) {
            updateAccessToken();
            return tryToLoginUserByEmail(email);
        }
    }


    private AuthServiceJwtResponse tryToLoginUserByEmail(String email) {
        try {
            return restClient
                    .post()
                    .uri("/auth/login-by-email")
                    .body(Map.of("email", email))
                    .header("Authorization", "Bearer " + correctJwtToken)
                    .retrieve()
                    .body(AuthServiceJwtResponse.class);
        } catch (HttpClientErrorException.NotFound notFoundException) {
            throw new UserNotFoundException("Пользователь с заданным email не найден");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden unauthorizedException) {
            throw new AccessToAuthServiceException(unauthorizedException.getMessage());
        }
    }

    @Override
    public AuthServiceJwtResponse loginUserByPhoneNumber(String phoneNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AuthServiceUserExistsResponse existsUserByUsernameOrEmail(AuthServiceExistsUserRequest authServiceExistsUserRequest) {
        try {
            return restClient
                    .post()
                    .uri("/api/users/exists")
                    .body(authServiceExistsUserRequest)
                    .retrieve()
                    .body(AuthServiceUserExistsResponse.class);
        } catch (HttpClientErrorException exception) {
            throw new AuthServiceException("При обращение к AuthService возникла ошибка: " + exception.getMessage());
        }
    }

    public void updateAccessToken() {
        Map<String, String> requestBody = Map.of(
                "clientId", clientId,
                "clientSecret", clientSecret
        );

        TokenResponse response = restClient.post()
                .uri("/auth/services/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(TokenResponse.class);

        if (response != null && response.getAccessToken() != null) {
            this.correctJwtToken = response.getAccessToken();
            log.info("Successfully updated service access token.");
        } else {
            log.error("Failed to update service access token.");
        }
    }

    @Getter
    @Setter
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
    }
}
