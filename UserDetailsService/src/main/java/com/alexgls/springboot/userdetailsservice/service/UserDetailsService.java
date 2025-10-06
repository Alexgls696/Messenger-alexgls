package com.alexgls.springboot.userdetailsservice.service;

import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.entity.UserDetailsResponse;
import reactor.core.publisher.Mono;

public interface UserDetailsService {
    Mono<UserDetailsResponse> findByUserId(int userId);

    Mono<UserDetailsResponse> update(UpdateUserDetailsRequest updateUserDetailsRequest);

    Mono<Void> deleteByUserId(int userId);
}
