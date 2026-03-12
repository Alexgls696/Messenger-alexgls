package com.alexgls.springboot.messagestorageservicevt.utils;

import lombok.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;


@RequiredArgsConstructor
public class AuthHttpClient {

    private final RestClient restClient;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public class AuthResponse {
        private String accessToken;
        private String refreshToken;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public class AuthRequest {
        private String username;
        private String password;
    }


    public AuthResponse authorize(String username, String password) {
        try {
            return restClient
                    .post()
                    .body(new AuthRequest(username, password))
                    .retrieve()
                    .body(AuthResponse.class);
        } catch (HttpClientErrorException exception) {
            throw new HttpClientErrorException(exception.getStatusCode(), exception.getResponseBodyAsString());
        }
    }

}
