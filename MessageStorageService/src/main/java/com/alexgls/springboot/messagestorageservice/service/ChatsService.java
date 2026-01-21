package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.dto.CreateGroupDto;
import com.alexgls.springboot.messagestorageservice.dto.MessageDto;
import com.alexgls.springboot.messagestorageservice.dto.UpdateGroupDto;
import com.alexgls.springboot.messagestorageservice.entity.Chat;
import com.alexgls.springboot.messagestorageservice.entity.ChatRole;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.entity.Participants;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservice.mapper.ChatMapper;
import com.alexgls.springboot.messagestorageservice.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservice.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservice.repository.DeletedMessagesRepository;
import com.alexgls.springboot.messagestorageservice.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservice.service.encryption.EncryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.TransactionOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatsService {

    private final ChatsRepository chatsRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessagesRepository messagesRepository;
    private final DeletedMessagesRepository deletedMessagesRepository;
    private final TransactionalOperator transactionalOperator;
    private final EncryptUtils encryptUtils;

    public Flux<ChatDto> findAllChatsByUserId(int userId, Pageable pageable) {
        int limit = pageable.getPageSize();
        long offset = pageable.getOffset();
        return chatsRepository.findChatsByUserId(userId, limit, offset)
                .concatMap(chat -> {
                    log.info("Chat {}", chat);
                    ChatDto chatDto = ChatMapper.toDto(chat);
                    Mono<Message> lastMessageInChat = messagesRepository.findLastMessageByChatIdAndUserId(chat.getChatId(), userId);
                    return Mono.zip(Mono.just(chatDto), lastMessageInChat)
                            .flatMap(tuple -> {
                                ChatDto chatdto = tuple.getT1();
                                MessageDto lastMessageDto = MessageMapper.toMessageDto(tuple.getT2());
                                lastMessageDto.setContent(encryptUtils.decrypt(lastMessageDto.getContent()));
                                chatdto.setLastMessage(lastMessageDto);
                                chatDto.setNumberOfUnreadMessages(chat.getUnreadCount());
                                return Mono.just(chatdto);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                chatDto.setNumberOfUnreadMessages(chat.getUnreadCount());
                                return Mono.just(chatDto);
                            }));
                });
    }


    //Создание или поиск личного чата
    public Mono<ChatDto> findOrCreatePrivateChat(int senderId, int receiverId) {
        return transactionalOperator.transactional(chatsRepository.findChatIdByParticipantsIdForPrivateChats(senderId, receiverId)
                .flatMap(existingChatId -> {
                    Mono<Chat> chatMono = chatsRepository.findById(existingChatId);
                    return chatMono.map(ChatMapper::toDto);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Chat newChat = new Chat();
                    newChat.setType("PRIVATE");
                    newChat.setCreatedAt(Timestamp.from(Instant.now()));
                    newChat.setUpdatedAt(Timestamp.from(Instant.now()));
                    return chatsRepository.save(newChat)
                            .flatMap(savedChat -> {
                                Participants p1 = new Participants();
                                p1.setUserId(senderId);
                                p1.setChatId(savedChat.getChatId());
                                p1.setJoinedAt(Timestamp.from(Instant.now()));

                                Participants p2 = new Participants();
                                p2.setUserId(receiverId);
                                p2.setChatId(savedChat.getChatId());
                                p2.setJoinedAt(Timestamp.from(Instant.now()));

                                return participantsRepository.saveAll(List.of(p1, p2))
                                        .then(Mono.just(ChatMapper.toDto(savedChat)));
                            });
                })));
    }


    public Mono<ChatDto> createGroup(CreateGroupDto createGroupDto, int creatorId) {
        Chat chat = ChatMapper.createGroupDtoToEntity(createGroupDto);
        return chatsRepository.save(chat)
                .flatMap(savedChat -> {
                    List<Participants> participants = createGroupDto.membersIds()
                            .stream()
                            .map(id -> createParticipantForGroup(ChatRole.MEMBER, id, savedChat.getChatId())).collect(Collectors.toList());
                    participants.add(createParticipantForGroup(ChatRole.OWNER, creatorId, savedChat.getChatId()));
                    return participantsRepository.saveAll(participants)
                            .then(Mono.just(ChatMapper.toDto(savedChat)));
                });
    }

    public Mono<ChatDto> updateGroup(UpdateGroupDto updateGroupDto, int actorId) {
        return participantsRepository.findByChatIdAndUserId(updateGroupDto.chatId(), actorId)
                .switchIfEmpty(Mono.error(() -> new NoSuchParticipantException("Не найдена связь между участником чата и самим чатом")))
                .flatMap(participant -> {
                    if (!ChatRole.CanEditGroupDescription(participant.getRole())) {
                        return Mono.error(() -> new AccessDeniedException("У вас нет доступа для выполнения данной операции"));
                    }
                    return chatsRepository.findById(updateGroupDto.chatId());
                })
                .switchIfEmpty(Mono.error(() -> new NoSuchUsersChatException("Чат с заданным id не найден")))
                .flatMap(chat -> {
                    chat.setName(updateGroupDto.name());
                    chat.setDescription(updateGroupDto.description());
                    return chatsRepository.save(chat);
                }).map(ChatMapper::toDto);
    }


    private Participants createParticipantForGroup(ChatRole chatRole, int userId, int chatId) {
        Participants participant = new Participants();
        participant.setRole(chatRole);
        participant.setUserId(userId);
        participant.setChatId(chatId);
        participant.setJoinedAt(Timestamp.from(Instant.now()));
        return participant;
    }


    public Mono<ChatDto> findById(int id, int currentUserId) {
        return chatsRepository.findById(id)
                .flatMap(chat -> {
                    ChatDto chatDto = ChatMapper.toDto(chat);
                    Mono<Message> messageMono = messagesRepository.findLastMessageByChatIdAndUserId(chat.getChatId(), currentUserId);
                    return Mono.zip(Mono.just(chatDto), messageMono)
                            .map(tuple -> {
                                var chat_dto = tuple.getT1();
                                var messageDto = MessageMapper.toMessageDto(tuple.getT2());
                                messageDto.setContent(encryptUtils.decrypt(messageDto.getContent()));
                                chat_dto.setLastMessage(messageDto);
                                return chat_dto;
                            });
                });
    }

    public Mono<Void> deleteChatById(int chatId, int userId) {
        return transactionalOperator.transactional(
                participantsRepository.findByChatIdAndUserId(chatId, userId)
                        .switchIfEmpty(Mono.error(() -> new NoSuchUsersChatException("Комбинация чата и его участника не найдена")))
                        .flatMap(participants -> {
                            participants.setDeletedByUser(true);
                            return participantsRepository.save(participants);
                        })
                        .flatMap(saved -> deletedMessagesRepository.markAllMessagesAsRemovedWhenChatRemoving(chatId, userId))
        ).then();
    }

    public Mono<Integer> findRecipientIdByChatId(int chatId, int senderId) {
        return chatsRepository.findRecipientIdByChatId(chatId, senderId);
    }

    public Mono<Integer> findChatIdByRecipientId(int recipientId, int myId) {
        return chatsRepository.findChatIdByUserId(recipientId, myId)
                .doOnError(error -> log.warn("Не удалось найти ваш чат с этим человеком: {}", error.getMessage()))
                .switchIfEmpty(Mono.just(0));
    }

}
