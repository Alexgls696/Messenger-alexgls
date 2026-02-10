package com.alexgls.springboot.messagestorageservice.client;

import com.alexgls.springboot.messagestorageservice.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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

    @Override
    public Flux<GetUserDto> findAllUsers(Iterable<Integer> ids, String token) {
        return webClient.post()
                .uri("api/users/find-by-ids")
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token))
                .bodyValue(ids)
                .retrieve()
                .bodyToFlux(GetUserDto.class);
    }
}
