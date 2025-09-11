package com.alexgls.springboot.messagestorageservice.client;

import com.alexgls.springboot.messagestorageservice.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AuthWebClientImpl implements AuthWebClient {

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
