package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessagesRepository extends CrudRepository<Message, Long> {

    List<Message> findAllByIdIn(Collection<Long> ids);

    @Query("select distinct m " +
            "from Message m " +
            "join Participants p on m.chatId = p.chat.id and p.userId = :currentUserId " +
            "left join DeletedMessage dm on m.id = dm.messageId and dm.userId = :currentUserId " +
            "where m.chatId = :chatId and dm.userId is null " +
            "and ((p.leave is false and p.removed is false) or (m.createdAt < p.removeAt)) " +
            "order by m.createdAt desc")
    Page<Message> findAllMessagesByChatId(@Param("chatId") int chatId,
                                          @Param("currentUserId") int currentUserId,
                                          Pageable pageable);


    @Query(value = "select m.* from messages m left join deleted_messages dm " +
            "on m.message_id = dm.message_id and dm.user_id = :currentUserId " +
            "where m.chat_id = :chatId and dm.user_id is null " +
            "order by created_at desc limit 1;", nativeQuery = true)
    Optional<Message> findLastMessageByChatIdAndUserId(@Param("chatId") long chatId, int currentUserId);

    @Modifying
    @Query("update Message set isRead = true, readAt = :now " +
            "where id in (:messageIds) ")
    void markMessagesAsRead(@Param("messageIds") List<Long> messageIds, @Param("now") Timestamp readAtNow);
}
