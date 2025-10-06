package com.alexgls.springboot.userdetailsservice.service;

import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.entity.UserDetailsResponse;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserDetailsException;
import com.alexgls.springboot.userdetailsservice.mapper.UserDetailsMapper;
import com.alexgls.springboot.userdetailsservice.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDetailsRepository userDetailsRepository;

    @Override
    public Mono<UserDetailsResponse> findByUserId(int userId) {
        return userDetailsRepository.findByUserId(userId)
                .map(UserDetailsMapper::toDto)
                .switchIfEmpty(Mono.error(new NoSuchUserDetailsException("Данные о пользователе с id %d не найдены".formatted(userId))));
    }

    @Override
    public Mono<UserDetailsResponse> update(UpdateUserDetailsRequest updateUserDetailsRequest) {
        return userDetailsRepository.findByUserId(updateUserDetailsRequest.userId())
                .flatMap(userDetails -> {
                    userDetails.setBirthday(updateUserDetailsRequest.birthday());
                    userDetails.setStatus(updateUserDetailsRequest.status());
                    return userDetailsRepository.save(userDetails);
                })
                .map(UserDetailsMapper::toDto);
    }

    @Override
    public Mono<Void> deleteByUserId(int userId) {
        return userDetailsRepository.deleteByUserId(userId);
    }
}
