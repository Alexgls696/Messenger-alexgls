package com.alexgls.springboot.notificationsservice.client;

import com.alexgls.springboot.notificationsservice.dto.ServiceLoginRequest;
import com.alexgls.springboot.notificationsservice.dto.ServiceLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
public class AuthServiceClientImpl implements AuthServiceClient {

    private final RestClient restClient;

    private final ParameterizedTypeReference<Map<String, String>> PARAMETERIZED_TYPE_REF = new ParameterizedTypeReference<Map<String, String>>() {
    };

    @Value("${security.id}")
    private String registerClientId;

    @Value("${security.secret}")
    private String registerClientSecret;

    @Override
    public ServiceLoginResponse getServiceAccessToken() {
        return restClient
                .post()
                .uri("/auth/services/token")
                .body(new ServiceLoginRequest(registerClientId, registerClientSecret))
                .retrieve()
                .body(ServiceLoginResponse.class);
    }
}
