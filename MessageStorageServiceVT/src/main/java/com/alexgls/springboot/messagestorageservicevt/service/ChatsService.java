package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.dto.*;
import com.alexgls.springboot.messagestorageservicevt.entity.Chat;
import com.alexgls.springboot.messagestorageservicevt.entity.ChatRole;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUserException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservicevt.mapper.ChatMapper;
import com.alexgls.springboot.messagestorageservicevt.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservicevt.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.DeletedMessagesRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatsService {

    private final ChatsRepository chatsRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessagesRepository messagesRepository;
    private final DeletedMessagesRepository deletedMessagesRepository;

    private final EncryptUtils encryptUtils;

    @Transactional(readOnly = true)
    public List<ChatDto> findAllChatsByUserId(int userId, Pageable pageable) {
        Page<ChatWithUnread> chats = chatsRepository.findChatsByUserId(userId, pageable);
        List<ChatDto> result = new ArrayList<>();
        for (ChatWithUnread chatWithUnread : chats.getContent()) {
            ChatDto chatDto = ChatMapper.toDto(chatWithUnread.chat());
            Optional<Message> messageOptional = messagesRepository.findLastMessageByChatIdAndUserId(chatWithUnread.chat().getId(), userId);
            if (messageOptional.isPresent()) {
                Message message = messageOptional.get();
                setLastMessageToChatDto(message, chatDto);
            }

            chatDto.setNumberOfUnreadMessages(chatWithUnread.unreadCount());
            result.add(chatDto);
        }
        return result;
    }

    @Transactional
    public ChatDto findOrCreatePrivateChat(int senderId, int receiverId) {
        Optional<Long> existingChatId = chatsRepository.findChatIdByParticipantsIdForPrivateChats(senderId, receiverId);
        if (existingChatId.isPresent()) {
            Chat chat = chatsRepository.findById(existingChatId.get())
                    .orElseThrow(() -> new NoSuchUsersChatException("Чат с заданным id не найден"));
            var chatDto = ChatMapper.toDto(chat);
            Optional<Message> messageOptional = messagesRepository.findLastMessageByChatIdAndUserId(chat.getId(), senderId);
            if (messageOptional.isPresent()) {
                Message message = messageOptional.get();
                setLastMessageToChatDto(message, chatDto);
            }
            return chatDto;
        } else {
            Chat chat = new Chat();
            chat.setType("PRIVATE");
            chat.setCreatedAt(Timestamp.from(Instant.now()));
            chat.setUpdatedAt(Timestamp.from(Instant.now()));

            Chat savedChat = chatsRepository.save(chat);
            Participants p1 = new Participants();
            p1.setUserId(senderId);
            p1.setChat(chat);
            p1.setJoinedAt(Timestamp.from(Instant.now()));
            p1.setRole(ChatRole.MEMBER);

            Participants p2 = new Participants();
            p2.setUserId(receiverId);
            p2.setChat(chat);
            p2.setJoinedAt(Timestamp.from(Instant.now()));
            p2.setRole(ChatRole.MEMBER);

            participantsRepository.saveAll(List.of(p1, p2));
            return ChatMapper.toDto(savedChat);
        }
    }

    private void setLastMessageToChatDto(Message message, ChatDto chatDto) {
        if (!Objects.isNull(message)) {
            MessageDto lastMessageDto = MessageMapper.toMessageDto(message);
            lastMessageDto.setContent(encryptUtils.decrypt(lastMessageDto.getContent()));
            chatDto.setLastMessage(lastMessageDto);
        }
    }


    @Transactional
    public ChatDto createGroup(CreateGroupDto createGroupDto, int creatorId) {
        Chat chat = ChatMapper.createGroupDtoToEntity(createGroupDto);
        Chat savedChat = chatsRepository.save(chat);
        List<Participants> participants = createGroupDto.membersIds()
                .stream()
                .map(id -> createParticipantForGroup(ChatRole.MEMBER, id, savedChat)).collect(Collectors.toList());
        participants.add(createParticipantForGroup(ChatRole.OWNER, creatorId, savedChat));

        participantsRepository.saveAll(participants);
        return ChatMapper.toDto(savedChat);
    }

    private Participants createParticipantForGroup(ChatRole chatRole, int userId, Chat chat) {
        Participants participant = new Participants();
        participant.setRole(chatRole);
        participant.setUserId(userId);
        participant.setChat(chat);
        participant.setJoinedAt(Timestamp.from(Instant.now()));
        return participant;
    }

    @Transactional
    public ChatDto updateGroup(UpdateGroupDto updateGroupDto, int actorId) {
        Participants participant = participantsRepository.findByChatIdAndUserId(updateGroupDto.chatId(), actorId)
                .orElseThrow(() -> new NoSuchParticipantException("Не найдена связь между участником чата и самим чатом"));
        if (!ChatRole.CanEditGroupDescription(participant.getRole())) {
            throw new AccessDeniedException("У вас нет доступа для выполнения данной операции");
        }
        Chat chat = chatsRepository.findById((long) updateGroupDto.chatId())
                .orElseThrow(() -> new NoSuchUsersChatException("Чат с заданным id не найден"));
        chat.setName(updateGroupDto.name());
        chat.setDescription(updateGroupDto.description());
        return ChatMapper.toDto(chatsRepository.save(chat));
    }

    public ChatDto findById(long chatId, int currentUserId) {
        Chat chat = chatsRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchUsersChatException("Чат с заданным id не найден"));
        Message lastMessage = messagesRepository.findLastMessageByChatIdAndUserId(chat.getId(), currentUserId)
                .orElse(null);
        ChatDto chatDto = ChatMapper.toDto(chat);
        if (!Objects.isNull(lastMessage)) {
            MessageDto messageDto = MessageMapper.toMessageDto(lastMessage);
            messageDto.setContent(encryptUtils.decrypt(messageDto.getContent()));
            chatDto.setLastMessage(messageDto);
        }
        return chatDto;
    }

    @Transactional
    public void deleteChatById(long chatId, int userId) {
        Participants participants = participantsRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new NoSuchParticipantException("Комбинация чата и его участника не найдена"));
        participants.setDeletedByUser(true);
        participantsRepository.save(participants);
        deletedMessagesRepository.markAllMessagesAsRemovedWhenChatRemoving(chatId, userId);
    }

    public Integer findRecipientIdByChatId(int chatId, int senderId) {
        return chatsRepository.findRecipientIdByChatId(chatId, senderId)
                .orElseThrow(() -> new NoSuchUserException("Участник чата не найден"));
    }

    public Integer findChatIdByRecipientId(int recipientId, int myId) {
        return chatsRepository.findChatIdByUserId(recipientId, myId)
                .orElseThrow(() -> new NoSuchUsersChatException("Чат не найден"));
    }

    public GroupAccessDto getUserRightsByGroupId(long groupId, int userId) {
        Participants participants = participantsRepository.findByChatIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NoSuchParticipantException("Не найдена связь между чатом и пользователем"));
        return SecurityUtils.determinateGroupAccess(participants.getRole());
    }

}
