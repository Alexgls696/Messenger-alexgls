package com.alexgls.springboot.userdetailsservice.client;

import com.alexgls.springboot.userdetailsservice.dto.GetUserDto;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AuthServiceClientImpl implements AuthServiceClient {

    private final WebClient webClient;

    @Override
    public Mono<GetUserDto> findUserById(int id, String token) {
        return webClient
                .get()
                .uri("api/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token))
                .retrieve()
                .bodyToMono(GetUserDto.class)
                .onErrorResume(WebClientResponseException.NotFound.class, exception -> Mono.error(new NoSuchUserException("User with id %d not found".formatted(id))));
    }
}
