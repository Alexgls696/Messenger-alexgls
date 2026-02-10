package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.MessageToken;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageTokenId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageTokenRepository extends CrudRepository<MessageToken, MessageTokenId> {

    @Query(value = "SELECT t.message_id FROM message_tokens t " +
            "JOIN messages m ON t.message_id = m.message_id " +
            "WHERE m.chat_id = :chatId AND t.token_hash IN (:hashes) " +
            "AND (m.message_id NOT IN (SELECT dm.message_id from deleted_messages dm where user_id = :userId))", nativeQuery = true)
    List<Long> findAllMessageIdsByTokenHashInChat(@Param("chatId") int chatId, @Param("userId") int userId, @Param("hashes") Collection<String> hashes);
}
