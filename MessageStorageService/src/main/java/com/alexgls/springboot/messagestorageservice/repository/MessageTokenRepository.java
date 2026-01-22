package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.MessageToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface MessageTokenRepository extends ReactiveCrudRepository<MessageToken, Long> {
    @Query(value = "SELECT t.message_id FROM message_tokens t " +
            "JOIN messages m ON t.message_id = m.message_id " +
            "WHERE m.chat_id = :chatId AND t.token_hash IN (:hashes) " +
            "AND (m.message_id NOT IN (SELECT dm.message_id from deleted_messages dm where user_id = :userId))")
    Flux<Long> findAllMessageIdsByTokenHashInChat(@Param("chatId") int chatId, @Param("userId") int userId, @Param("hashes") Collection<String> hashes);

}
