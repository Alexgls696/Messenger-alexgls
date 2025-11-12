package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.*;
import com.alexgls.springboot.messagestorageservice.entity.*;
import com.alexgls.springboot.messagestorageservice.exceptions.DeleteMessageAccessDeniedException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchRecipientException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservice.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservice.repository.*;
import com.alexgls.springboot.messagestorageservice.service.encryption.EncryptUtils;
import com.alexgls.springboot.messagestorageservice.service.nlp.LexicalAnalyzer;
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
    private final MessageTokenRepository messageTokenRepository;

    private final TransactionalOperator transactionalOperator;

    private final EncryptUtils encryptUtils;
    private final LexicalAnalyzer lexicalAnalyzer;

    public Flux<Message> getMessagesByChatId(int chatId, int page, int pageSize, int currentUserId) {
        return messagesRepository.findAllMessagesByChatId(chatId, page, pageSize, currentUserId)
                .flatMap(message -> {
                    Mono<List<Attachment>> attachments = attachmentRepository.findAllByMessageId(message.getId()).collectList();
                    return Mono.zip(Mono.just(message), attachments)
                            .map(tuple -> {
                                var mes = tuple.getT1();
                                mes.setContent(encryptUtils.decrypt(mes.getContent()));
                                var attachmentsList = tuple.getT2();
                                mes.setAttachments(attachmentsList);
                                return mes;
                            });
                })
                .sort(Comparator.comparing(Message::getCreatedAt));
    }

    public Flux<MessageDto> findMessagesByContent(SearchMessageInChatRequest request) {
        var lemmas = lexicalAnalyzer.lemmatizeText(request.content());
        var hashes = lemmas.stream()
                .map(encryptUtils::calculateHmac)
                .toList();

        return messageTokenRepository.findAllMessageIdsByTokenHashInChat(request.chatId(), hashes)
                .collectList()
                .flatMapMany(messagesRepository::findAllByIdIn)
                .map(MessageMapper::toMessageDto)
                .map(messageDto -> {
                    messageDto.setContent(encryptUtils.decrypt(messageDto.getContent()));
                    return messageDto;
                });
    }


    public Mono<Long> readMessagesByList(List<ReadMessagePayload> messages) {
        return Flux.fromIterable(messages)
                .flatMap(message -> messagesRepository.readMessagesByList(message.messageId()))
                .count();
    }

    public Flux<MessageDto> encryptAllMessages() {
        return transactionalOperator.transactional(messagesRepository.findAll()
                .flatMap(message -> {
                    Mono<Message> encryptMessage = processAndEncryptMessage(message);
                    return encryptMessage
                            .flatMap(processedMessage ->
                                    messagesRepository.save(processedMessage)
                                            .flatMap(this::saveMessageTokens)
                            );
                }).map(MessageMapper::toMessageDto));
    }


    public Mono<MessageDto> save(CreateMessagePayload createMessagePayload) {
        Mono<Chat> chatMono = chatsRepository.findById(createMessagePayload.chatId())
                .switchIfEmpty(Mono.error(new NoSuchUsersChatException("Chat with id " + createMessagePayload.chatId() + " not found")));

        return transactionalOperator.transactional(chatMono.flatMap(chat -> {
                    Message message = createMessageFromPayload(createMessagePayload);

                    Mono<Message> processedMessageMono = processAndEncryptMessage(message);

                    Mono<Message> savedMessageMono = processedMessageMono
                            .flatMap(processedMessage ->
                                    messagesRepository.save(processedMessage)
                                            .flatMap(this::saveMessageTokens)
                            );

                    if (chat.isGroup()) {
                        return savePublicGroupMessage(createMessagePayload, savedMessageMono);
                    } else {
                        return savePrivateChatMessage(createMessagePayload, savedMessageMono);
                    }
                })
                .flatMap(messageDto ->
                        removeMarkIsDeletedForChatAndUserId(createMessagePayload)
                                .thenReturn(messageDto)
                ));
    }

    private Mono<MessageDto> savePublicGroupMessage(CreateMessagePayload createMessagePayload, Mono<Message> savedMessageMono) {
        return savedMessageMono.flatMap(savedMessage ->
                saveAttachmentsPayloadsToDatabase(createMessagePayload.attachments(), savedMessage.getId(), createMessagePayload.chatId())
                        .map(savedAttachments -> {
                            MessageDto dto = MessageMapper.toMessageDto(savedMessage);
                            dto.setAttachments(savedAttachments);
                            dto.setType(savedMessage.getType());
                            dto.setTempId(createMessagePayload.tempId());
                            dto.setContent(encryptUtils.decrypt(savedMessage.getContent()));
                            return dto;
                        })
        );
    }

    private Mono<MessageDto> savePrivateChatMessage(CreateMessagePayload createMessagePayload, Mono<Message> savedMessageMono) {
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
                                dto.setContent(encryptUtils.decrypt(savedMessage.getContent()));
                                dto.setAttachments(savedAttachments);
                                dto.setType(savedMessage.getType());
                                dto.setTempId(createMessagePayload.tempId());
                                return dto;
                            });
                });
    }

    private Mono<Message> saveMessageTokens(Message savedMessage) {
        if (savedMessage.getTokenHashes() == null || savedMessage.getTokenHashes().isEmpty()) {
            return Mono.just(savedMessage); // Токенов нет, просто продолжаем
        }

        List<MessageToken> tokensToSave = savedMessage.getTokenHashes().stream()
                .map(hash -> new MessageToken(savedMessage.getId(), hash))
                .toList();

        return messageTokenRepository.saveAll(tokensToSave)
                .then(Mono.just(savedMessage));
    }

    private Mono<Message> processAndEncryptMessage(Message message) {
        if (message.getContent() == null || message.getContent().isEmpty()) {
            return Mono.just(message);
        }
        return Mono.just(message)
                .publishOn(Schedulers.boundedElastic())
                .map(msgToProcess -> {
                    String originalText = msgToProcess.getContent();

                    List<String> lemmas = lexicalAnalyzer.lemmatizeText(originalText);

                    Set<String> tokenHashes = new HashSet<>();
                    for (String lemma : lemmas) {
                        tokenHashes.add(encryptUtils.calculateHmac(lemma));
                    }

                    String encryptedText = encryptUtils.encrypt(originalText);

                    msgToProcess.setContent(encryptedText);
                    msgToProcess.setTokenHashes(tokenHashes);

                    return msgToProcess;
                });
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


    Mono<Void> removeMarkIsDeletedForChatAndUserId(CreateMessagePayload createMessagePayload) {
        return participantsRepository.findUserIdsWhoDeletedChat(createMessagePayload.chatId())
                .flatMap(id -> participantsRepository.removeMarkIsDeletedForChatAndUserId(createMessagePayload.chatId(), id))
                .doOnError(error -> log.warn("Failed to remove 'is_deleted' mark: {}", error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<DeleteMessageResponse> deleteById(DeleteMessageRequest deleteMessageRequest, int currentUserId) {
        return transactionalOperator.transactional(messagesRepository.findAllById(deleteMessageRequest.messagesId())
                .collectList()
                .flatMap(messagesList -> {
                    if (deleteMessageRequest.forAll()) {
                        return deleteMessageForAll(deleteMessageRequest, messagesList, currentUserId);
                    } else {
                        return deleteMessageForCurrentUser(deleteMessageRequest, messagesList, currentUserId);
                    }
                }));
    }

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
