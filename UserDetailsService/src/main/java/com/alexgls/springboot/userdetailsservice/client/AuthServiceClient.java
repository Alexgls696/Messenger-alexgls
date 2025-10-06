package com.alexgls.springboot.userdetailsservice.client;

import com.alexgls.springboot.userdetailsservice.dto.GetUserDto;
import reactor.core.publisher.Mono;

public interface AuthServiceClient {
    Mono<GetUserDto> findUserById(int id, String token);
}
