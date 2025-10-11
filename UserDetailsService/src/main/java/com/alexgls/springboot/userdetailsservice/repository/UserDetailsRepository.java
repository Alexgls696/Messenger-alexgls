package com.alexgls.springboot.userdetailsservice.repository;


import com.alexgls.springboot.userdetailsservice.entity.UserDetails;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserDetailsRepository extends ReactiveCrudRepository<UserDetails, Integer> {
    Mono<UserDetails> findByUserId(Integer userId);

    Mono<Void> deleteByUserId(Integer userId);

    Mono<Boolean>existsByUserId(Integer userId);
}
