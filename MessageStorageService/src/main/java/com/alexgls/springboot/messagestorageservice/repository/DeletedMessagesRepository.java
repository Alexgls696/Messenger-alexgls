package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.DeletedMessage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DeletedMessagesRepository extends ReactiveCrudRepository<DeletedMessage, Long> {
    Mono<Void> deleteAllByMessageId(Long messageId);

    @Query(value = "select dm.user_id from deleted_messages dm where message_id = :messageId")
    Flux<Integer> findAllUserIdByMessageId(@Param("messageId") Long messageId);
}
