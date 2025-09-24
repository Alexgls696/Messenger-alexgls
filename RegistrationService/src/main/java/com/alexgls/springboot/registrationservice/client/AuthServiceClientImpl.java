package com.alexgls.springboot.registrationservice.client;

import com.alexgls.springboot.registrationservice.dto.AuthServiceExistsUserRequest;
import com.alexgls.springboot.registrationservice.dto.AuthServiceJwtResponse;
import com.alexgls.springboot.registrationservice.dto.AuthServiceUserExistsResponse;
import com.alexgls.springboot.registrationservice.dto.UserRegisterDto;
import com.alexgls.springboot.registrationservice.exception.AuthServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
public class AuthServiceClientImpl implements AuthServiceClient {
    private final RestClient restClient;

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
}
