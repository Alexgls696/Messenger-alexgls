package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.DeletedMessage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeletedMessagesRepository extends CrudRepository<DeletedMessage, Long> {
    @Modifying
    void deleteAllByMessageId(Long messageId);

    @Modifying
    void deleteAllByMessageIdIn(List<Long> messageIds);


    @Query("select userId from DeletedMessage where messageId = :deleteMessageId")
    List<Integer> findAllUserIdByMessageId(@Param("deleteMessageId") Long messageId);

    @Modifying
    @Query(value = "insert into deleted_messages (message_id, user_id) " +
            "select m.message_id, :userId from messages m where chat_id = :chatId " +
            "on conflict (message_id, user_id) do nothing ", nativeQuery = true)
    void markAllMessagesAsRemovedWhenChatRemoving(@Param("chatId") long chatId, @Param("userId") int userId);
}
