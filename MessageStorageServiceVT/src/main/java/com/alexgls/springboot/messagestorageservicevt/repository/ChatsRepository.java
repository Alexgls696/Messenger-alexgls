package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.dto.ChatWithUnread;
import com.alexgls.springboot.messagestorageservicevt.entity.Chat;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ChatsRepository extends CrudRepository<Chat, Long> {


    @Query("""
                select new com.alexgls.springboot.messagestorageservicevt.dto.ChatWithUnread(c, p.unreadCount)
                from Chat c
                join c.participants p
                where p.userId = :userId
                  and p.isDeletedByUser = false
                order by c.updatedAt desc
            """)
    @BatchSize(size = 50)
    Page<ChatWithUnread> findChatsByUserId(
            @Param("userId") int userId,
            Pageable pageable
    );


    @Query(value = """
            SELECT p1.chat_id
            FROM participants p1
            JOIN participants p2 ON p1.chat_id = p2.chat_id
            JOIN chats c on p1.chat_id = c.chat_id
            WHERE p1.user_id = :senderId AND p2.user_id = :receiverId and c.is_group = false
              AND p1.chat_id IN (
                SELECT chat_id
                FROM participants
                GROUP BY chat_id
                HAVING COUNT(user_id) = 2
              );""", nativeQuery = true)
    Optional<Long> findChatIdByParticipantsIdForPrivateChats(@Param("senderId") int senderId,
                                                             @Param("receiverId") int receiverId);

    @Query(value = "select distinct(c.chat_id) from chats c " +
            "join participants p1 on c.chat_id = p1.chat_id " +
            "join participants p2 on p1.chat_id = p2.chat_id " +
            "where c.type = 'PRIVATE' and p1.user_id = :userId and p2.user_id = :myId", nativeQuery = true)
    Optional<Integer> findChatIdByUserId(int userId, int myId);

    @Query(value = "select p.user_id from participants p join public.chats c on p.chat_id = c.chat_id " +
            "where c.chat_id = :chatId and user_id != :senderId and is_group = false", nativeQuery = true)
    Optional<Integer> findRecipientIdByChatId(@Param("chatId") int chatId, @Param("senderId") int senderId);

    @Modifying
    @Query(value = "UPDATE chats SET last_message_id = :newLastMessageId, updated_at = now() " +
            "WHERE chat_id = :currentChatId",
            nativeQuery = true)
    void updateLastMessageIdByChatId(@Param("currentChatId") int currentChatId,
                                     @Param("newLastMessageId") long newLastMessageId);
}
