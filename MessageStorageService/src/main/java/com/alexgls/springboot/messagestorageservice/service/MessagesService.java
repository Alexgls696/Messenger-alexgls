package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.*;
import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import com.alexgls.springboot.messagestorageservice.entity.DeletedMessage;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.entity.MessageType;
import com.alexgls.springboot.messagestorageservice.exceptions.DeleteMessageAccessDeniedException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchRecipientException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservice.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagesService {

    private final MessagesRepository messagesRepository;
    private final DeletedMessagesRepository deletedMessagesRepository;
    private final ChatsRepository chatsRepository;
    private final AttachmentRepository attachmentRepository;
    private final ParticipantsRepository participantsRepository;

    private final TransactionalOperator transactionalOperator;

    public Flux<Message> getMessagesByChatId(int chatId, int page, int pageSize, int currentUserId) {
        return messagesRepository.findAllMessagesByChatId(chatId, page, pageSize, currentUserId)
                .flatMap(message -> {
                    Mono<List<Attachment>> attachments = attachmentRepository.findAllByMessageId(message.getId()).collectList();
                    return Mono.zip(Mono.just(message), attachments)
                            .map(tuple -> {
                                var mes = tuple.getT1();
                                var attachmentsList = tuple.getT2();
                                log.info(mes.toString());
                                mes.setAttachments(attachmentsList);
                                return mes;
                            });
                })
                .sort(Comparator.comparing(Message::getCreatedAt));
    }


    public Mono<Long> readMessagesByList(List<ReadMessagePayload> messages) {
        return Flux.fromIterable(messages)
                .flatMap(message -> messagesRepository.readMessagesByList(message.messageId()))
                .count();
    }


    public Mono<MessageDto> save(CreateMessagePayload createMessagePayload) {
        return transactionalOperator.transactional(chatsRepository.existsById(createMessagePayload.chatId())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new NoSuchUsersChatException("Chat with id " + createMessagePayload.chatId() + " not found")))
                .then(Mono.defer(() -> {
                    Message message = createMessageFromPayload(createMessagePayload);
                    Mono<Message> savedMessageMono = messagesRepository.save(message);
                    Mono<Integer> recipientIdMono = chatsRepository.findRecipientIdByChatId(
                            createMessagePayload.chatId(),
                            createMessagePayload.senderId()
                    ).switchIfEmpty(Mono.error(new NoSuchRecipientException("Recipient not found for chat " + createMessagePayload.chatId())));
                    return Mono.zip(savedMessageMono, recipientIdMono)
                            .flatMap(tuple -> {
                                Message savedMessage = tuple.getT1();
                                int recipientId = tuple.getT2();
                                savedMessage.setRecipientId(recipientId);

                                return saveAttachmentsPayloadsToDatabase(createMessagePayload.attachments(), savedMessage.getId(), createMessagePayload.chatId())
                                        .map(savedAttachments -> {
                                            MessageDto dto = MessageMapper.toMessageDto(savedMessage);
                                            dto.setAttachments(savedAttachments);
                                            dto.setType(message.getType());
                                            dto.setTempId(createMessagePayload.tempId());
                                            return dto;
                                        });
                            })
                            .flatMap(messageDto ->
                                    removeMarkIsDeletedForChatAndUserId(createMessagePayload)
                                            .thenReturn(messageDto)
                            );
                })));
    }

    Mono<Void> removeMarkIsDeletedForChatAndUserId(CreateMessagePayload createMessagePayload) {
        return participantsRepository.findUserIdsWhoDeletedChat(createMessagePayload.chatId())
                .flatMap(id -> participantsRepository.removeMarkIsDeletedForChatAndUserId(createMessagePayload.chatId(), id))
                .doOnError(error -> log.warn("Failed to remove 'is_deleted' mark: {}", error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Message createMessageFromPayload(CreateMessagePayload payload) {
        Message message = new Message();
        message.setCreatedAt(Timestamp.from(Instant.now()));
        message.setContent(payload.content());
        message.setType(
                (payload.attachments() == null || payload.attachments().isEmpty())
                        ? MessageType.TEXT
                        : MessageType.FILE
        );
        message.setSenderId(payload.senderId());
        message.setChatId(payload.chatId());
        return message;
    }

    @Transactional
    public Mono<List<Attachment>> saveAttachmentsPayloadsToDatabase(List<CreateAttachmentPayload> attachmentPayloads, long messageId, int chatId) {
        if (attachmentPayloads == null || attachmentPayloads.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        List<Attachment> attachments = attachmentPayloads.stream()
                .map(payload -> {
                    Attachment attachment = new Attachment();
                    attachment.setMessageId(messageId);
                    attachment.setFileId(payload.fileId());
                    attachment.setMimeType(payload.mimeType());
                    attachment.setChatId(chatId);
                    attachment.setFileName(payload.fileName());
                    attachment.setLogicType(MessageType.fromMimeType(payload.mimeType()));
                    return attachment;
                }).toList();

        return attachmentRepository.saveAll(attachments)
                .collectList();
    }

    @Transactional
    public Mono<DeleteMessageResponse> deleteById(DeleteMessageRequest deleteMessageRequest, int currentUserId) {
        return messagesRepository.findAllById(deleteMessageRequest.messagesId())
                .collectList()
                .flatMap(messagesList -> {
                    if (deleteMessageRequest.forAll()) {
                        return deleteMessageForAll(deleteMessageRequest, messagesList, currentUserId);
                    } else {
                        return deleteMessageForCurrentUser(deleteMessageRequest, messagesList, currentUserId);
                    }
                });
    }

    @Transactional
    public Mono<DeleteMessageResponse> deleteMessageForAll(DeleteMessageRequest deleteMessageRequest, List<Message> messagesList, int currentUserId) {
        List<Long> messagesIdsToDeleteList = new ArrayList<>();
        for (var message : messagesList) {
            if (message.getSenderId() == currentUserId) {
                messagesIdsToDeleteList.add(message.getId());
            } else {
                return Mono.error(new DeleteMessageAccessDeniedException("Данный пользователь не может выполнить это действие."));
            }
        }
        Mono<Void> deleteAllDeletedMessagesForUsers = deleteAllDeletedMessagesForUsers(deleteMessageRequest);
        Mono<Void> deleteAllAttachmentsMono = deleteAllAttachmentsByMessageIdMono(messagesIdsToDeleteList);
        Mono<Void> deleteAllMessagesMono = messagesRepository.deleteAllById(messagesIdsToDeleteList);
        return deleteAllDeletedMessagesForUsers
                .then(deleteAllAttachmentsMono)
                .then(deleteAllMessagesMono)
                .then(generateDeleteMessageResponseWithChatMembers(deleteMessageRequest));
    }

    @Transactional
    public Mono<DeleteMessageResponse> deleteMessageForCurrentUser(DeleteMessageRequest deleteMessageRequest, List<Message> messagesList, int currentUserId) {
        List<DeletedMessage> deletedMessages = messagesList.stream()
                .map(message -> new DeletedMessage(null, message.getId(), currentUserId))
                .toList();
        return deletedMessagesRepository.saveAll(deletedMessages)
                .then(generateDeleteMessageResponseWithChatMembers(deleteMessageRequest))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(response -> {
                    // Запуск асинхронной задачи удаления сообщений
                    checkAndDeleteFullyDeletedMessages(response.messagesId(), response.chatId())
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnError(error -> log.warn("Delete messages check failed: {}", error.getMessage()))
                            .subscribe();
                });
    }

    public Mono<Void> deleteAllAttachmentsByMessageIdMono(List<Long> messagesIds) {
        return Flux.fromIterable(messagesIds)
                .flatMap(attachmentRepository::deleteAllByMessageId)
                .then(Mono.empty());
    }


    private Mono<Void> deleteAllDeletedMessagesForUsers(DeleteMessageRequest deleteMessageRequest) {
        return Flux.fromIterable(deleteMessageRequest.messagesId())
                .flatMap(deletedMessagesRepository::deleteAllByMessageId)
                .then(Mono.empty());
    }


    @Transactional
    public Mono<Void> checkAndDeleteFullyDeletedMessages(List<Long> messageIds, int chatId) {
        return Flux.fromIterable(messageIds)
                .flatMap(messageId -> deleteMessageIfItDeletedForEveryone(messageId, chatId))
                .then();
    }

    private Mono<DeleteMessageResponse> generateDeleteMessageResponseWithChatMembers(DeleteMessageRequest deleteMessageRequest) {
        return participantsRepository.findUserIdsByChatId(deleteMessageRequest.chatId())
                .collectList()
                .map(membersIdsList -> new DeleteMessageResponse(deleteMessageRequest.messagesId(),
                        membersIdsList,
                        deleteMessageRequest.senderId(),
                        deleteMessageRequest.chatId(),
                        deleteMessageRequest.forAll()));
    }

    @Transactional
    public Mono<Void> deleteMessageIfItDeletedForEveryone(long messageId, int chatId) {
        return participantsRepository.findUserIdsByChatId(chatId).collectList()
                .zipWith(deletedMessagesRepository.findAllUserIdByMessageId(messageId).collect(Collectors.toSet()))
                .flatMap(tuple -> {
                    List<Integer> participants = tuple.getT1();
                    Set<Integer> usersWhoDeleted = tuple.getT2();
                    if (participants.size() == usersWhoDeleted.size() &&
                            usersWhoDeleted.containsAll(participants)) {
                        log.info("All participants deleted message {}, removing completely", messageId);
                        return deletedMessagesRepository.deleteAllByMessageId(messageId)
                                .then(messagesRepository.deleteById(messageId));
                    }
                    return Mono.empty();
                });
    }
}
