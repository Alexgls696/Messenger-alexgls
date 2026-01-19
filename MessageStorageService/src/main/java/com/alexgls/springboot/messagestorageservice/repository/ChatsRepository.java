package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.dto.ChatWithUnread;
import com.alexgls.springboot.messagestorageservice.entity.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatsRepository extends ReactiveCrudRepository<Chat, Integer> {
    @Query(value = """
            SELECT c.*, p.unread_count FROM chats c
            JOIN participants p ON p.chat_id = c.chat_id 
            WHERE p.user_id = :userId and is_deleted_by_user = false
            ORDER BY c.updated_at DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ChatWithUnread> findChatsByUserId(
            @Param("userId") int userId,
            @Param("limit") int limit,
            @Param("offset") long offset);


    @Query("SELECT p1.chat_id\n" +
            "FROM participants p1\n" +
            "JOIN participants p2 ON p1.chat_id = p2.chat_id\n" +
            "WHERE p1.user_id = :senderId AND p2.user_id = :receiverId\n" +
            "  AND p1.chat_id IN (\n" +
            "    SELECT chat_id\n" +
            "    FROM participants\n" +
            "    GROUP BY chat_id\n" +
            "    HAVING COUNT(user_id) = 2\n" +
            "  );")
    Mono<Integer> findChatIdByParticipantsIdForPrivateChats(@Param("senderId") int senderId,
                                                            @Param("receiverId") int receiverId);

    @Query(value = "select distinct(c.chat_id) from chats c " +
            "join participants p1 on c.chat_id = p1.chat_id " +
            "join participants p2 on p1.chat_id = p2.chat_id " +
            "where c.type = 'PRIVATE' and p1.user_id = :userId and p2.user_id = :myId")
    Mono<Integer>findChatIdByUserId(int userId, int myId);

    @Query(value = "select p.user_id from participants p join public.chats c on p.chat_id = c.chat_id where c.chat_id = :chatId and user_id != :senderId and is_group = false")
    Mono<Integer> findRecipientIdByChatId(@Param("chatId") int chatId, @Param("senderId") int senderId);

    @Query(value = "update chats set last_message_id =:lastMessageId where chat_id =:chatId")
    Mono<Void>updateLastMessageIdByChatId(@Param("chatId") int chatId, @Param("lastMessageId") long lastMessageId);

}
