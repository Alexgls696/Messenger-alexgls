package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageType;
import com.alexgls.springboot.messagestorageservicevt.repository.projection.AttachmentsByMessagesListProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends CrudRepository<Attachment, Long> {

    List<Attachment> findAllByMessageId(long messageId);

    @Query("select a.messageId as messageId, a as attachment from Attachment a "+
            "where a.messageId in :messagesIds")
    List<AttachmentsByMessagesListProjection> findAllByMessageIds(@Param("messagesIds") List<Long>messagesIds);

    @Query("""
     select a from Attachment a
     join Message m on a.messageId = m.id
     join Participants p on p.chat.id = m.chatId
     where a.logicType = :messageType
     and a.chatId = :chatId
     and p.userId = :userId
     and a.messageId not in (
         select dm.messageId from DeletedMessage dm where dm.userId = :userId
     )
     and m.createdAt <= COALESCE(p.removeAt, CURRENT_TIMESTAMP)
     order by m.createdAt desc
    """)
    List<Attachment> findAllByLogicTypeAndChatId(
            @Param("messageType") MessageType messageType,
            @Param("chatId") long chatId,
            @Param("userId") int userId
    );

    @Modifying
    void deleteAllByMessageIdIn(List<Long> messageIds);
}
