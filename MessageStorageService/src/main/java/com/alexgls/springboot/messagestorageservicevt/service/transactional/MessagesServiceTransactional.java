package com.alexgls.springboot.messagestorageservicevt.service.transactional;

import com.alexgls.springboot.messagestorageservicevt.dto.messages.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.DeleteMessageRequest;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.DeleteMessageResponse;
import com.alexgls.springboot.messagestorageservicevt.entity.DeletedMessage;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.exceptions.DeleteMessageAccessDeniedException;
import com.alexgls.springboot.messagestorageservicevt.repository.AttachmentRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.DeletedMessagesRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.projection.UserIdWhenDeletedMessageProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessagesServiceTransactional {

    private final MessagesRepository messagesRepository;
    private final DeletedMessagesRepository deletedMessagesRepository;
    private final AttachmentRepository attachmentRepository;
    private final ParticipantsRepository participantsRepository;


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
        deleteAllAttachmentsByMessageId(messagesIdsToDeleteList);
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

    private DeleteMessageResponse generateDeleteMessageResponseWithChatMembers(DeleteMessageRequest deleteMessageRequest) {
        List<Integer> userIds = participantsRepository.findUserIdsByChatId(deleteMessageRequest.chatId());
        return new DeleteMessageResponse(deleteMessageRequest.messagesId(),
                userIds,
                deleteMessageRequest.senderId(),
                deleteMessageRequest.chatId(),
                deleteMessageRequest.forAll());
    }

    //Удаляет метку удаленного чата для пользователя, который удалил его для себя
    @Transactional
    public void removeMarkIsDeletedForChatAndUserId(CreateMessagePayload createMessagePayload) {
        List<Integer> userIdsWhoDeletedChat = participantsRepository.findUserIdsWhoDeletedChat(createMessagePayload.chatId());
        participantsRepository.removeMarkIsDeletedForChatAndUserIdForAll(userIdsWhoDeletedChat, createMessagePayload.chatId());
    }


    @Transactional
    protected void deleteAllAttachmentsByMessageId(List<Long> messagesIds) {
        attachmentRepository.deleteAllByMessageIdIn(messagesIds);
    }

    @Transactional
    protected void deleteAllDeletedMessagesForUsers(DeleteMessageRequest deleteMessageRequest) {
        deletedMessagesRepository.deleteAllByMessageIdIn(deleteMessageRequest.messagesId());
    }

}
