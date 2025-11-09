package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.MessageToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface MessageTokenRepository extends ReactiveCrudRepository<MessageToken, Long> {
}
