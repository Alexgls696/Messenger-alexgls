package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import com.alexgls.springboot.messagestorageservice.entity.MessageType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AttachmentRepository extends ReactiveCrudRepository<Attachment, Long> {
    Flux<Attachment> findAllByMessageId(Long messageId);

    Flux<Attachment> findAllByLogicTypeAndChatId(MessageType messageType, int chatId);

    Mono<Void> deleteAllByMessageId(Long messageId);
}
