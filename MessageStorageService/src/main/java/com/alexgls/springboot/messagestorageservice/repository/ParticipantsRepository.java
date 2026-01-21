package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Participants;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ParticipantsRepository extends ReactiveCrudRepository<Participants, Integer> {
    @Query(value = "select * from participants where chat_id = :chatId")
    Flux<Participants> findByChatId(@Param("chatId") Integer chatId);


    @Query(value = "select user_id from participants where chat_id = :chatId")
    Flux<Integer> findUserIdsByChatId(@Param("chatId") Integer chatId);

    Mono<Participants> findByChatIdAndUserId(int chatId, int userId);

    @Modifying
    @Query("update participants set is_deleted_by_user = false where chat_id = :chatId and user_id = :userId")
    Mono<Void> removeMarkIsDeletedForChatAndUserId(@Param("chatId") int chatId, @Param("userId") int userId);

    @Query("select participants.user_id from participants where chat_id = :chatId")
    Flux<Integer> findUserIdsWhoDeletedChat(@Param("chatId") int chatId);

    @Modifying
    @Query(value = "update participants set unread_count = unread_count + 1 where chat_id = :chatId and user_id != :senderId ")
    Mono<Void> incrementUpdateCountForUser(@Param("chatId") int chatId, @Param("senderId") int senderId);


    @Modifying
    @Query(value = "update participants set unread_count = 0 where chat_id = :chatId and user_id = :readerId ")
    Mono<Void> resetCountForCurrentUser(@Param("chatId") int chatId, @Param("readerId") int readerId);



    @Modifying
    @Query("""
                UPDATE participants
                SET unread_count = GREATEST(0, unread_count - :count),
                    last_read_message_id = GREATEST(last_read_message_id, :lastMessageId)
                WHERE chat_id = :chatId AND user_id = :userId
            """)
    Mono<Void> updateUnreadCountAndLastMessageId(
            @Param("chatId") int chatId,
            @Param("userId") int userId,
            @Param("lastMessageId") long lastMessageId,
            @Param("count") int count
    );

}
