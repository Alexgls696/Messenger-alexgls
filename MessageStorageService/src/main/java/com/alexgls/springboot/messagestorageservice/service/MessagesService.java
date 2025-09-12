package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.*;
import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.entity.MessageType;
import com.alexgls.springboot.messagestorageservice.exceptions.DeleteMessageAccessDeniedException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchRecipientException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservice.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservice.repository.AttachmentRepository;
import com.alexgls.springboot.messagestorageservice.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagesService {

    private final MessagesRepository messagesRepository;
    private final ChatsService chatsService;
    private final AttachmentRepository attachmentRepository;
    private final ParticipantsService participantsService;

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


    @Transactional
    public Mono<MessageDto> save(CreateMessagePayload createMessagePayload) {
        return chatsService.existsById(createMessagePayload.chatId())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new NoSuchUsersChatException("Chat with id " + createMessagePayload.chatId() + " not found")))
                .then(Mono.defer(() -> {
                    Message message = createMessageFromPayload(createMessagePayload);
                    Mono<Message> savedMessageMono = messagesRepository.save(message);
                    Mono<Integer> recipientIdMono = chatsService.findRecipientIdByChatId(
                            createMessagePayload.chatId(),
                            createMessagePayload.senderId()
                    ).switchIfEmpty(Mono.error(new NoSuchRecipientException("Recipient not found for chat " + createMessagePayload.chatId())));

                    return Mono.zip(savedMessageMono, recipientIdMono)
                            .flatMap(tuple -> {
                                Message savedMessage = tuple.getT1();
                                Integer recipientId = tuple.getT2();
                                savedMessage.setRecipientId(recipientId);
                                Mono<Void> updateLastMessageInChatMono = chatsService.updateLastMessageToChat(createMessagePayload.chatId(), savedMessage.getId());
                                return updateLastMessageInChatMono
                                        .then(Mono.defer(() -> saveAttachmentsPayloadsToDatabase(createMessagePayload.attachments(), savedMessage.getId(), createMessagePayload.chatId())))
                                        .map(savedAttachments -> {
                                            MessageDto dto = MessageMapper.toMessageDto(savedMessage);
                                            dto.setAttachments(savedAttachments);
                                            dto.setType(message.getType());
                                            dto.setTempId(createMessagePayload.tempId());
                                            return dto;
                                        });
                            });
                }));
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

    private Mono<List<Attachment>> saveAttachmentsPayloadsToDatabase(List<CreateAttachmentPayload> attachmentPayloads, long messageId, int chatId) {
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

    public Mono<DeleteMessageResponse> deleteById(DeleteMessageRequest deleteMessageRequest, int currentUserId) {
        return messagesRepository.findAllById(deleteMessageRequest.messagesId())
                .collectList()
                .flatMap(list -> {
                    if (deleteMessageRequest.forAll()) {
                        List<Long> messagesIdsToDeleteList = new ArrayList<>();
                        for (var message : list) {
                            if (message.getSenderId() == currentUserId) {
                                messagesIdsToDeleteList.add(message.getId());
                            } else {
                                return Mono.error(new DeleteMessageAccessDeniedException("Данный пользователь не может выполнить это действие."));
                            }
                        }
                        return messagesRepository.deleteAllById(messagesIdsToDeleteList)
                                .then(generateDeleteMessageResponseWithChatMembers(deleteMessageRequest));
                    } else {
                        List<Message> messagesDeletedToCurrentUserList = list
                                .stream()
                                .peek(message -> message.setDeletedForUserId(currentUserId))
                                .toList();
                        return messagesRepository.saveAll(messagesDeletedToCurrentUserList)
                                .then(generateDeleteMessageResponseWithChatMembers(deleteMessageRequest));
                    }
                });
    }

    private Mono<DeleteMessageResponse> generateDeleteMessageResponseWithChatMembers(DeleteMessageRequest deleteMessageRequest) {
        return participantsService.findUserIdsByChatId(deleteMessageRequest.chatId())
                .collectList()
                .map(membersIdsList -> new DeleteMessageResponse(deleteMessageRequest.messagesId(),
                        membersIdsList,
                        deleteMessageRequest.senderId(),
                        deleteMessageRequest.chatId(),
                        deleteMessageRequest.forAll()));
    }
}
