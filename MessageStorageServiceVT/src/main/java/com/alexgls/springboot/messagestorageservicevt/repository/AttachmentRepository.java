package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends CrudRepository<Attachment, Long> {

    List<Attachment> findAllByMessageId(Long messageId);


    @Query("""
             from Attachment
             where logicType = :messageType and chatId =:chatId
             and messageId not in (select dm.messageId from DeletedMessage dm where dm.userId = :userId)
            """)
    List<Attachment> findAllByLogicTypeAndChatId(@Param("messageType") MessageType messageType, @Param("chatId") long chatId, @Param("userId") int userId);

    @Modifying
    void deleteAllByMessageId(Long messageId);

    @Modifying
    void deleteAllByMessageIdIn(List<Long> messageIds);
}
