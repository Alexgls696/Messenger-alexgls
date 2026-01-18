package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Message;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface

MessagesRepository extends ReactiveCrudRepository<Message, Long> {

    Flux<Message>findAllByIdIn(Collection<Long> ids);

    @Query("select distinct m.* from messages m left join deleted_messages dm on m.message_id = dm.message_id and dm.user_id = :currentUserId " +
            "where m.chat_id = :chatId and dm.user_id is null " +
            "order by created_at desc limit :size offset :page")
    Flux<Message> findAllMessagesByChatId(@Param("chatId") int chatId,
                                          @Param("page") int page,
                                          @Param("size") int size,
                                          @Param("currentUserId") int currentUserId);

    @Modifying
    @Query("update messages set is_read = true, read_at = now() where message_id = :messageId")
    Mono<Void> readMessagesByList(@Param("messageId") int messageId);


    @Query("select m.* from messages m left join deleted_messages dm " +
            "on m.message_id = dm.message_id and dm.user_id = :currentUserId " +
            "where m.chat_id = :chatId and dm.user_id is null " +
            "order by created_at desc limit 1;")
    Mono<Message> findLastMessageByChatIdAndUserId(@Param("chatId") int chatId, int currentUserId);

}
