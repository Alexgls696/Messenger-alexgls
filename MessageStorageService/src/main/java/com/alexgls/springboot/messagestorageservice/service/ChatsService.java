package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.dto.MessageDto;
import com.alexgls.springboot.messagestorageservice.entity.Chat;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.entity.Participants;
import com.alexgls.springboot.messagestorageservice.mapper.ChatMapper;
import com.alexgls.springboot.messagestorageservice.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservice.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservice.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatsService {

    private final ChatsRepository chatsRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessagesRepository messagesRepository;

    public Flux<ChatDto> findAllChatsByUserId(int userId, Pageable pageable) {
        int limit = pageable.getPageSize();
        long offset = pageable.getOffset();
        return chatsRepository.findChatsByUserId(userId, limit, offset)
                .flatMap(chat -> {
                    log.info("Chat {}", chat);
                    ChatDto chatDto = ChatMapper.toDto(chat);
                    Mono<Message> lastMessageInChat = messagesRepository.findLastMessageByChatId(chat.getChatId());
                    return Mono.zip(Mono.just(chatDto), lastMessageInChat)
                            .flatMap(tuple -> {
                                ChatDto chatdto = tuple.getT1();
                                MessageDto lastMessageDto = MessageMapper.toMessageDto(tuple.getT2());
                                chatdto.setLastMessage(lastMessageDto);
                                return Mono.just(chatdto);
                            });
                });
    }

    @Transactional
    public Mono<ChatDto> findOrCreatePrivateChat(int senderId, int receiverId) {
        return chatsRepository.findChatIdByParticipantsIdForPrivateChats(senderId, receiverId)
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
                }));
    }

    public Mono<Boolean> existsById(Integer id) {
        return chatsRepository.existsById(id);
    }

    public Mono<ChatDto> findById(int id) {
        return chatsRepository.findById(id)
                .flatMap(chat -> {
                    ChatDto chatDto = ChatMapper.toDto(chat);
                    Mono<Message> messageMono = messagesRepository.findLastMessageByChatId(chat.getChatId());
                    return Mono.zip(Mono.just(chatDto), messageMono)
                            .map(tuple -> {
                                var chat_dto = tuple.getT1();
                                var messageDto = MessageMapper.toMessageDto(tuple.getT2());
                                chat_dto.setLastMessage(messageDto);
                                return chat_dto;
                            });
                });
    }

    public Mono<Integer> findRecipientIdByChatId(int chatId, int senderId) {
        return chatsRepository.findRecipientIdByChatId(chatId, senderId);
    }

    public Flux<Integer> findRecipientIdsByChatId(int chatId, int senderId) {
        return chatsRepository.findRecipientIdsByChatId(chatId, senderId);
    }

    public Mono<Void> updateLastMessageToChat(int chatId, long messageId) {
        return chatsRepository.updateLastMessageId(chatId, messageId);
    }

}
