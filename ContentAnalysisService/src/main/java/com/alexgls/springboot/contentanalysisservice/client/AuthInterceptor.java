package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.service.GigaChatTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements ClientHttpRequestInterceptor {

    private final GigaChatTokenManager tokenManager;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String token = tokenManager.getToken();
        request.getHeaders().setBearerAuth(token);

        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            tokenManager.invalidateToken(token);

            String newToken = tokenManager.refreshToken();
            request.getHeaders().setBearerAuth(newToken);

            return execution.execute(request, body);
        }

        return response;
    }
}