package com.alexgls.springboot.messagestorageservice.client;

import com.alexgls.springboot.messagestorageservice.dto.GetUserDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AuthWebClient {
    Mono<GetUserDto> findUserById(int id, String token);

    Flux<GetUserDto> findAllUsers(Iterable<Integer> ids,  String token);
}
