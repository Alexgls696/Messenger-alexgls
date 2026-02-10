package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends CrudRepository<Attachment, Long> {

    List<Attachment> findAllByMessageId(Long messageId);

    List<Attachment> findAllByLogicTypeAndChatId(MessageType messageType, int chatId);

    @Modifying
    Void deleteAllByMessageId(Long messageId);

    @Modifying
    void deleteAllByMessageIdIn(List<Long> messageIds);
}
