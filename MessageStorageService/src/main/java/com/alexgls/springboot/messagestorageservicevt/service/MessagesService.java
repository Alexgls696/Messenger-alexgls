package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.dto.attachments.CreateAttachmentPayload;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.*;
import com.alexgls.springboot.messagestorageservicevt.entity.*;
import com.alexgls.springboot.messagestorageservicevt.exceptions.DeleteMessageAccessDeniedException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchRecipientException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservicevt.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservicevt.repository.*;
import com.alexgls.springboot.messagestorageservicevt.repository.projection.AttachmentsByMessagesListProjection;
import com.alexgls.springboot.messagestorageservicevt.repository.projection.UserIdWhenDeletedMessageProjection;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import com.alexgls.springboot.messagestorageservicevt.service.nlp.LexicalAnalyzer;
import com.alexgls.springboot.messagestorageservicevt.util.groups.ServiceMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private final EncryptUtils encryptUtils;
    private final LexicalAnalyzer lexicalAnalyzer;


    public record ReadMessageDatabaseRequest
            (
                    List<Long> messageIds,
                    int chatId,
                    int readerId,
                    long lastReadMessageId,
                    int countMessagesRead
            ) {
    }

    public List<MessageDto> getMessagesByChatId(int chatId, int page, int pageSize, int currentUserId) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Message> messages = messagesRepository.findAllMessagesByChatId(chatId, currentUserId, pageable);
        List<Long> messagesIds = messages.stream().map(Message::getId).toList();
        Participants participants = participantsRepository.findByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new NoSuchParticipantException("Вы не состоите в этом чате или чат не существует"));
        Map<Long, List<Attachment>> attachmentsMap = attachmentRepository.findAllByMessageIds(messagesIds)
                .stream()
                .collect(Collectors.groupingBy(AttachmentsByMessagesListProjection::getMessageId, Collectors.mapping(AttachmentsByMessagesListProjection::getAttachment, Collectors.toList())));

        for (var message : messages) {
            message.setContent(encryptUtils.decrypt(message.getContent()));
            var attachments = attachmentsMap.getOrDefault(message.getId(), Collections.emptyList());
            message.setAttachments(attachments);
            if (!Objects.isNull(participants.getLastReadMessageId())) {
                if (message.getSenderId() == currentUserId) {
                    message.setRead(true);
                } else {
                    boolean isReadByCurrentUser = message.getId() <= participants.getLastReadMessageId();
                    message.setRead(isReadByCurrentUser);
                }
            }
        }
        return messages.stream()
                .map(MessageMapper::toMessageDto)
                .sorted(Comparator.comparing(MessageDto::getCreatedAt))
                .toList();

    }

    public MessageDto findById(long messageId, long chatId, int sender) {
        Participants participants = participantsRepository.findByChatIdAndUserId(chatId, sender)
                .orElseThrow(() -> new NoSuchParticipantException("Вы не принадлежите этому чату"));
        return messagesRepository.findById(messageId)
                .map(MessageMapper::toMessageDto)
                .map(msq -> {
                    msq.setContent(encryptUtils.decrypt(msq.getContent()));
                    return msq;
                })
                .orElseThrow(() -> new NoSuchMessageException("Сообщение не найдено"));
    }

    public List<MessageDto> findMessagesByContent(SearchMessageInChatRequest request, int userId) {
        var lemmas = lexicalAnalyzer.lemmatizeText(request.content());
        var hashes = lemmas.stream()
                .map(encryptUtils::calculateHmac)
                .toList();
        if (!hashes.isEmpty()) {
            List<Long> messageIds = messageTokenRepository.findAllMessageIdsByTokenHashInChat(request.chatId(), userId, hashes);
            return messagesRepository.findAllByIdInOrderById(messageIds)
                    .stream()
                    .map(message -> {
                        MessageDto messageDto = MessageMapper.toMessageDto(message);
                        messageDto.setContent(encryptUtils.decrypt(message.getContent()));
                        return messageDto;
                    })
                    .toList();
        }
        return List.of();
    }

    @Transactional
    public void readMessagesByList(List<ReadMessagePayload> messages, int readerId) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        int chatId = messages.get(0).chatId();
        List<Long> messageIds = messages.stream()
                .map(ReadMessagePayload::messageId)
                .toList();

        long lastReadMessageId = Collections.max(messageIds);
        int countMessagesRead = messageIds.size();
        readMessages(new ReadMessageDatabaseRequest(messageIds, chatId, readerId, lastReadMessageId, countMessagesRead));
    }

    @Transactional
    public MessageDto updateMessage(long messageId, int userId, EditMessageRequest editMessageRequest) {
        Message message = messagesRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchMessageException("Сообщение не найдено"));
        Chat chat = chatsRepository.findById(editMessageRequest.chatId())
                .orElseThrow(() -> new NoSuchUsersChatException("Чат не найден"));

        if (message.getSenderId() != userId) {
            throw new AccessDeniedException("У вас нет доступа для выполнения данной операции. ");
        }
        message.setContent(editMessageRequest.content());
        message.setUpdatedAt(Timestamp.from(Instant.now()));
        processAndEncryptMessage(message);
        var savedMessage = messagesRepository.save(message);
        messageTokenRepository.deleteAllByMessageId(messageId);
        saveMessageTokens(savedMessage);

        var messageDto = MessageMapper.toMessageDto(message);
        messageDto.setContent(encryptUtils.decrypt(message.getContent()));
        messageDto.setAttachments(message.getAttachments());

        if (chat.isGroup()) {
            List<Integer> participants = participantsRepository.findUserIdsByChatIdWhenUsersNotDeleted((int) chat.getId());
            messageDto.setRecipientIds(participants);
        } else {
            Integer recipientId = chatsRepository.findRecipientIdByChatId((int) chat.getId(), userId)
                    .orElseThrow(() -> new NoSuchRecipientException("Участник чата не найден " + chat.getId()));
            messageDto.setRecipientId(recipientId);
        }
        return messageDto;
    }

    @Transactional
    protected void readMessages(final ReadMessageDatabaseRequest request) {
        messagesRepository.markMessagesAsRead(request.messageIds, Timestamp.from(Instant.now()));
        participantsRepository.updateUnreadCountAndLastMessageId(
                request.chatId,
                request.readerId,
                request.lastReadMessageId,
                request.countMessagesRead
        );

        var participants = participantsRepository.findByChatIdAndUserId(request.chatId, request.readerId)
                .orElseThrow(() -> new NoSuchParticipantException("Связь чата с пользователе не найдена."));
        if (participants.getLastReadMessageId() == request.lastReadMessageId) {
            participants.setUnreadCount(0);
        }
        participantsRepository.save(participants);
    }

    @Transactional
    public MessageDto save(CreateMessagePayload createMessagePayload) {
        Participants participants = participantsRepository.findByChatIdAndUserId(createMessagePayload.chatId(), createMessagePayload.senderId())
                .orElseThrow(() -> new NoSuchParticipantException("Чат с указанным id: %d и участником: %d не найден".formatted(createMessagePayload.chatId(), createMessagePayload.senderId())));
        if (participants.isRemoved() || participants.isLeave()) {
            throw new AccessDeniedException("У вас нет доступа для выполнения данной операции.");
        }
        Chat chat = chatsRepository.findById(participants.getChat().getId())
                .orElseThrow(() -> new NoSuchUsersChatException("Чат с указанным id: %d не найден".formatted(participants.getChat().getId())));
        Message message = MessageMapper.toMessageFromCreateMessagePayload(createMessagePayload);
        Message encryptedMessage = processAndEncryptMessage(message);
        Message savedMessage = messagesRepository.save(encryptedMessage);
        chat.setLastMessage(savedMessage);
        saveMessageTokens(savedMessage);
        MessageDto messageDto;
        if (chat.isGroup()) {
            messageDto = savePublicGroupMessage(createMessagePayload, savedMessage);
        } else {
            messageDto = savePrivateChatMessage(createMessagePayload, savedMessage);
        }
        removeMarkIsDeletedForChatAndUserId(createMessagePayload);
        chatsRepository.updateLastMessageIdByChatId(messageDto.getChatId(), messageDto.getId());
        participantsRepository.incrementUpdateCountForUser(message.getChatId(), messageDto.getSenderId());
        participantsRepository.resetCountForCurrentUser(message.getChatId(), messageDto.getSenderId());
        return messageDto;
    }


    //TODO CRITICAL! N+1 SAVING
    @Transactional
    public List<MessageDto> saveMessageWithForwardedMessages(CreateMessagePayload createMessagePayload, List<Long> forwardedMessageIds) {
        Participants participants = participantsRepository.findByChatIdAndUserId(createMessagePayload.chatId(), createMessagePayload.senderId())
                .orElseThrow(() -> new NoSuchParticipantException("Участник не найден"));

        if (participants.isRemoved() || participants.isLeave()) {
            throw new AccessDeniedException("Доступ запрещен");
        }

        Chat chat = chatsRepository.findById(createMessagePayload.chatId())
                .orElseThrow(() -> new NoSuchUsersChatException("Чат не найден"));

        List<Message> originalMessages = messagesRepository.findAllByIdInOrderById(forwardedMessageIds);
        List<MessageDto> resultDtos = new ArrayList<>();

        List<Integer> recipientsIds = null;
        Integer recipientId = null;
        if (chat.isGroup()) {
            recipientsIds = participantsRepository.findUserIdsByChatIdWhenUsersNotDeleted(createMessagePayload.chatId());
        } else {
            recipientId = chatsRepository.findRecipientIdByChatId(createMessagePayload.chatId(), createMessagePayload.senderId())
                    .orElseThrow(() -> new NoSuchRecipientException("Участник чата не найден " + createMessagePayload.chatId()));
        }

        for (Message original : originalMessages) {
            Message forwardedMsg = new Message();
            forwardedMsg.setChatId(chat.getId());
            forwardedMsg.setSenderId(createMessagePayload.senderId());
            forwardedMsg.setContent(original.getContent());
            forwardedMsg.setType(original.getType());
            forwardedMsg.setCreatedAt(Timestamp.from(Instant.now()));
            forwardedMsg.setRead(false);
            forwardedMsg.setForwarded(true);
            forwardedMsg.setForwardFromUserId(original.getSenderId());

            Message savedForwardedMsg = messagesRepository.save(forwardedMsg);

            List<Attachment> clonedAttachments = copyAttachmentsForNewMessage(original.getId(), savedForwardedMsg.getId(), chat.getId());

            MessageDto forwardedDto = MessageMapper.toMessageDto(savedForwardedMsg);
            forwardedDto.setAttachments(clonedAttachments);
            forwardedDto.setContent(encryptUtils.decrypt(savedForwardedMsg.getContent()));
            forwardedDto.setRecipientId(recipientId);
            forwardedDto.setRecipientIds(recipientsIds);
            resultDtos.add(forwardedDto);
        }

        if (createMessagePayload.content() != null && !createMessagePayload.content().isBlank()) {
            MessageDto mainMessageDto = this.save(createMessagePayload);
            resultDtos.add(mainMessageDto);
        } else {
            if (!resultDtos.isEmpty()) {
                MessageDto lastForwarded = resultDtos.get(resultDtos.size() - 1);
                removeMarkIsDeletedForChatAndUserId(createMessagePayload);
                chatsRepository.updateLastMessageIdByChatId(chat.getId(), lastForwarded.getId());
                participantsRepository.incrementUpdateCountForUser(chat.getId(), createMessagePayload.senderId());
                participantsRepository.resetCountForCurrentUser(createMessagePayload.chatId(), createMessagePayload.senderId());
            }
        }

        return resultDtos;
    }

    private List<Attachment> copyAttachmentsForNewMessage(long oldMessageId, long newMessageId, long newChatId) {
        List<Attachment> oldAttachments = attachmentRepository.findAllByMessageId(oldMessageId);

        if (oldAttachments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Attachment> newAttachments = oldAttachments.stream()
                .map(oldAttr -> {
                    Attachment copy = new Attachment();
                    copy.setMessageId(newMessageId);
                    copy.setChatId(newChatId);
                    copy.setFileId(oldAttr.getFileId());
                    copy.setMimeType(oldAttr.getMimeType());
                    copy.setFileName(oldAttr.getFileName());
                    copy.setLogicType(oldAttr.getLogicType());
                    copy.setHasAnalysis(oldAttr.getHasAnalysis());
                    return copy;
                }).toList();

        return (List<Attachment>) attachmentRepository.saveAll(newAttachments);
    }

    @Transactional
    public MessageDto saveServiceMessage(ServiceMessage serviceMessage, int chatId, int senderId) {
        CreateMessagePayload createMessagePayload = new CreateMessagePayload(chatId, senderId, serviceMessage.getMessage(),
                null, "service", null);
        return save(createMessagePayload);
    }

    private MessageDto savePublicGroupMessage(CreateMessagePayload createMessagePayload, Message savedMessage) {
        MessageDto dto = createMessageDto(createMessagePayload, savedMessage);
        List<Integer> participants = participantsRepository.findUserIdsByChatIdWhenUsersNotDeleted(dto.getChatId());
        dto.setRecipientIds(participants);
        return dto;
    }

    @Transactional
    protected MessageDto savePrivateChatMessage(CreateMessagePayload createMessagePayload, Message savedMessage) {
        Integer recipientId = chatsRepository.findRecipientIdByChatId(createMessagePayload.chatId(), createMessagePayload.senderId())
                .orElseThrow(() -> new NoSuchRecipientException("Участник чата не найден " + createMessagePayload.chatId()));
        savedMessage.setRecipientId(recipientId);
        return createMessageDto(createMessagePayload, savedMessage);
    }

    private MessageDto createMessageDto(CreateMessagePayload createMessagePayload, Message savedMessage) {
        List<Attachment> savedAttachments = saveAttachmentsPayloadsToDatabase(createMessagePayload.attachments(), savedMessage.getId(), createMessagePayload.chatId());
        MessageDto dto = MessageMapper.toMessageDto(savedMessage);
        dto.setAttachments(savedAttachments);
        dto.setTempId(createMessagePayload.tempId());
        dto.setContent(encryptUtils.decrypt(savedMessage.getContent()));
        return dto;
    }

    public List<Attachment> saveAttachmentsPayloadsToDatabase(List<CreateAttachmentPayload> attachmentPayloads, long messageId, long chatId) {
        if (attachmentPayloads == null || attachmentPayloads.isEmpty()) {
            return Collections.emptyList();
        }
        List<Attachment> attachments = attachmentPayloads.stream()
                .map(payload -> {
                    Attachment attachment = new Attachment();
                    attachment.setHasAnalysis(payload.hasAnalysis());
                    attachment.setMessageId(messageId);
                    attachment.setFileId(payload.fileId());
                    attachment.setMimeType(payload.mimeType());
                    attachment.setChatId(chatId);
                    attachment.setFileName(payload.fileName());
                    attachment.setLogicType(MessageType.fromMimeType(payload.mimeType()));
                    return attachment;
                }).toList();


        Iterable<Attachment> savedAttachments = attachmentRepository.saveAll(attachments);
        List<Attachment> result = new ArrayList<>();
        for (Attachment attachment : savedAttachments) {
            result.add(attachment);
        }
        return result;
    }


    private Message processAndEncryptMessage(Message message) {
        if (message.getContent() == null || message.getContent().isEmpty()) {
            return message;
        }
        String originalText = message.getContent();
        String encryptedText = encryptUtils.encrypt(originalText);
        message.setContent(encryptedText);
        return message;
    }

    @Transactional
    protected void saveMessageTokens(Message message) {
        if (message.getContent() == null || message.getContent().isEmpty() || message.isService()) {
            return;
        }
        String originalText = encryptUtils.decrypt(message.getContent());
        List<String> lemmas = lexicalAnalyzer.lemmatizeText(originalText);

        Set<String> uniqueHashes = new HashSet<>();
        for (String lemma : lemmas) {
            String hash = encryptUtils.calculateHmac(lemma);
            uniqueHashes.add(hash);
        }

        List<MessageToken> tokens = new ArrayList<>();
        for (String hash : uniqueHashes) {
            MessageToken token = new MessageToken(message.getId(), hash);
            tokens.add(token);
        }

        messageTokenRepository.saveAll(tokens);
    }


    //Удаляет метку удаленного чата для пользователя, который удалил его для себя
    public void removeMarkIsDeletedForChatAndUserId(CreateMessagePayload createMessagePayload) {
        List<Integer> userIdsWhoDeletedChat = participantsRepository.findUserIdsWhoDeletedChat(createMessagePayload.chatId());
        participantsRepository.removeMarkIsDeletedForChatAndUserIdForAll(userIdsWhoDeletedChat, createMessagePayload.chatId());
    }

    @Transactional
    public DeleteMessageResponse deleteMessages(DeleteMessageRequest deleteMessageRequest, int currentUserId) {
        Iterable<Message> messagesIterable = messagesRepository.findAllById(deleteMessageRequest.messagesId());
        List<Message> messages = new ArrayList<>();
        for (Message message : messagesIterable) {
            messages.add(message);
        }
        if (deleteMessageRequest.forAll()) {
            return deleteMessageForAll(deleteMessageRequest, messages, currentUserId);
        } else {
            return deleteMessageForCurrentUser(deleteMessageRequest, messages, currentUserId);
        }
    }

    @Transactional
    public DeleteMessageResponse deleteMessageForAll(DeleteMessageRequest deleteMessageRequest, List<Message> messagesList, int currentUserId) {
        List<Long> messagesIdsToDeleteList = new ArrayList<>();
        for (var message : messagesList) {
            if (message.getSenderId() == currentUserId) {
                messagesIdsToDeleteList.add(message.getId());
            } else {
                throw new DeleteMessageAccessDeniedException("Данный пользователь не может выполнить это действие.");
            }
        }

        deleteAllDeletedMessagesForUsers(deleteMessageRequest);
        deleteAllAttachmentsByMessageIdMono(messagesIdsToDeleteList);
        messagesRepository.deleteAllById(messagesIdsToDeleteList);
        participantsRepository.decrementUpdateCountForUser(deleteMessageRequest.chatId(), currentUserId);
        return generateDeleteMessageResponseWithChatMembers(deleteMessageRequest);
    }

    @Transactional
    public DeleteMessageResponse deleteMessageForCurrentUser(DeleteMessageRequest deleteMessageRequest, List<Message> messagesList, int currentUserId) {
        List<DeletedMessage> deletedMessages = messagesList.stream()
                .map(message -> new DeletedMessage(null, message.getId(), currentUserId))
                .toList();

        deletedMessagesRepository.saveAll(deletedMessages);
        DeleteMessageResponse response = generateDeleteMessageResponseWithChatMembers(deleteMessageRequest);
        checkAndDeleteFullyDeletedMessagesOptimization(response.messagesId(), response.chatId());
        return response;
    }

    private DeleteMessageResponse generateDeleteMessageResponseWithChatMembers(DeleteMessageRequest deleteMessageRequest) {
        List<Integer> userIds = participantsRepository.findUserIdsByChatId(deleteMessageRequest.chatId());
        return new DeleteMessageResponse(deleteMessageRequest.messagesId(),
                userIds,
                deleteMessageRequest.senderId(),
                deleteMessageRequest.chatId(),
                deleteMessageRequest.forAll());
    }


    @Transactional
    protected void deleteAllAttachmentsByMessageIdMono(List<Long> messagesIds) {
        attachmentRepository.deleteAllByMessageIdIn(messagesIds);
    }

    @Transactional
    protected void deleteAllDeletedMessagesForUsers(DeleteMessageRequest deleteMessageRequest) {
        deletedMessagesRepository.deleteAllByMessageIdIn(deleteMessageRequest.messagesId());
    }


    /**
     * Оптимизированный метод удаления сообщений для всех
     * @param messageIds - id сообщений, которые были удалены пользователем
     * @param chatId - чат, в котором происходит удаление сообщений
     */
    @Transactional
    public void checkAndDeleteFullyDeletedMessagesOptimization(List<Long> messageIds, int chatId) {
        List<Integer> participantsIds = participantsRepository.findUserIdsByChatId(chatId);
        Map<Long, Set<Integer>> messageIdUserMap = deletedMessagesRepository.findAllUserIdByMessageId(messageIds)
                .stream()
                .collect(Collectors.groupingBy(UserIdWhenDeletedMessageProjection::getMessageId,
                        Collectors.mapping(UserIdWhenDeletedMessageProjection::getUserId, Collectors.toSet())));
        List<Long>messagesToRemove = new ArrayList<>();
        for(var messageId : messageIds) {
            Set<Integer> userWhenDeleteMessage = messageIdUserMap.getOrDefault(messageId, Collections.emptySet());
            if(participantsIds.size() == userWhenDeleteMessage.size()
                    && userWhenDeleteMessage.containsAll(participantsIds)) {
                messagesToRemove.add(messageId);
            }
        }
        deletedMessagesRepository.deleteAllByMessageIdIn(messagesToRemove);
        messagesRepository.deleteAllById(messagesToRemove);
    }

    @Transactional
    protected void checkAndDeleteFullyDeletedMessages(List<Long> messageIds, int chatId) {
        for (var messageId : messageIds) {
            deleteMessageIfItDeletedForEveryone(messageId, chatId);
        }
    }

    @Transactional
    public void deleteMessageIfItDeletedForEveryone(long messageId, int chatId) {
        List<Integer> participantsIds = participantsRepository.findUserIdsByChatId(chatId);
        Set<Integer> userIdsWhenDeletedMessages = new HashSet<>(deletedMessagesRepository.findAllUserIdByMessageId(messageId));
        if (participantsIds.size() == userIdsWhenDeletedMessages.size() &&
                userIdsWhenDeletedMessages.containsAll(participantsIds)) {
            log.info("All participants deleted message {}, removing completely", messageId);
            deletedMessagesRepository.deleteAllByMessageId(messageId);
            messagesRepository.deleteById(messageId);
        }
    }


}
