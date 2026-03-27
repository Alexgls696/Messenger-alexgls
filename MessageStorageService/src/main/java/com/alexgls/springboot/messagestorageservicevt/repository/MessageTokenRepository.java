package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.MessageToken;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageTokenId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageTokenRepository extends CrudRepository<MessageToken, MessageTokenId> {

    @Modifying
    @Query("delete MessageToken where id.messageId = :messageId")
    void deleteAllByMessageId(@Param("messageId") long messageId);

    @Query(value = "SELECT t.message_id FROM message_tokens t " +
            "JOIN messages m ON t.message_id = m.message_id " +
            "JOIN participants p ON p.chat_id = m.chat_id AND p.user_id = :userId " +
            "WHERE m.chat_id = :chatId " +
            "AND t.token_hash IN (:hashes) " +
            "AND m.created_at <= COALESCE(p.remove_at, CURRENT_TIMESTAMP) " +
            "AND (m.message_id NOT IN (SELECT dm.message_id from deleted_messages dm where dm.user_id = :userId))",
            nativeQuery = true)
    List<Long> findAllMessageIdsByTokenHashInChat(@Param("chatId") int chatId, @Param("userId") int userId, @Param("hashes") Collection<String> hashes);
}
