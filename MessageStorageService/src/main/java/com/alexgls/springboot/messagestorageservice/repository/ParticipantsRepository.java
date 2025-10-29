package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Participants;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ParticipantsRepository extends ReactiveCrudRepository<Participants, Integer> {
    @Query(value = "select user_id from participants where chat_id = :chatId")
    Flux<Integer> findUserIdsByChatId(@Param("chatId") Integer chatId);

    Mono<Participants> findByChatIdAndUserId(int chatId, int userId);

    @Modifying
    @Query("update participants set is_deleted_by_user = false where chat_id = :chatId and user_id = :userId")
    Mono<Void> removeMarkIsDeletedForChatAndUserId(@Param("chatId") int chatId, @Param("userId") int userId);

    @Query("select participants.user_id from participants where chat_id = :chatId")
    Flux<Integer> findUserIdsWhoDeletedChat(@Param("chatId") int chatId);
}
