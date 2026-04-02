package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.client.ConnectionsServiceRestClient;
import com.alexgls.springboot.messagestorageservicevt.dto.CheckOnlineRequest;
import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.dto.chats.*;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.MessageDto;
import com.alexgls.springboot.messagestorageservicevt.dto.notifications.CreateNotificationRequest;
import com.alexgls.springboot.messagestorageservicevt.dto.notifications.NotificationType;
import com.alexgls.springboot.messagestorageservicevt.entity.*;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUserException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservicevt.mapper.ChatMapper;
import com.alexgls.springboot.messagestorageservicevt.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservicevt.repository.*;
import com.alexgls.springboot.messagestorageservicevt.repository.projection.RecipientProjection;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils;
import com.alexgls.springboot.messagestorageservicevt.util.groups.CreateGroupServiceMessage;
import com.alexgls.springboot.messagestorageservicevt.util.groups.InviteGroupServiceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Get;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.Part;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatsService {
    private final ChatsRepository chatsRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessagesRepository messagesRepository;
    private final DeletedMessagesRepository deletedMessagesRepository;
    private final PinnedChatsRepository pinnedChatsRepository;

    private final KafkaSenderService kafkaSenderService;
    private final MessagesService messagesService;

    private final AuthRestClient authRestClient;
    private final ConnectionsServiceRestClient connectionsServiceRestClient;

    private final MessageMapper messageMapper;

    @Transactional(readOnly = true)
    public List<ChatDto> findAllChats(int userId, String token, Pageable pageable) {
        Page<ChatWithUnread> chatPage = chatsRepository.findChatsByUserId(userId, pageable);
        List<ChatWithUnread> content = chatPage.getContent();
        List<Long> chatIds = content.stream()
                .map(c -> c.chat().getId()).toList();

        Map<Long, Message> lastMessagesMap = messagesRepository.findLastMessagesByChatIds(chatIds, userId)
                .stream().collect(Collectors.toMap(Message::getChatId, m -> m));

        Set<Long> pinnedChatIds = pinnedChatsRepository.findAllById_UserId(userId)
                .stream().map(pc -> pc.getId().getChatId()).collect(Collectors.toSet());

        Map<Long, Integer> chatToRecipientIdMap = participantsRepository
                .findRecipientsProjections(userId, chatIds)
                .stream()
                .collect(Collectors.toMap(
                        RecipientProjection::getChatId,
                        RecipientProjection::getUserId
                ));

        List<Integer> allRecipientIds = new ArrayList<>(chatToRecipientIdMap.values());

        Map<Integer, GetUserDto> userProfilesMap = authRestClient.findAllUsers(allRecipientIds, token)
                .stream()
                .collect(Collectors.toMap(GetUserDto::getId, u -> u));

        Map<Integer, Boolean> onlineStatuses = connectionsServiceRestClient.findUserOnlineStatus(new CheckOnlineRequest(allRecipientIds));
        return content.stream().map(c -> {
                    ChatDto dto = ChatMapper.toDto(c.chat());
                    long chatId = c.chat().getId();

                    if (lastMessagesMap.containsKey(chatId)) {
                        setLastMessageToChatDto(lastMessagesMap.get(chatId), dto);
                    }

                    dto.setNumberOfUnreadMessages(c.unreadCount());
                    dto.setPinned(pinnedChatIds.contains(chatId));

                    Integer recipientId = chatToRecipientIdMap.get(chatId);
                    if (recipientId != null && userProfilesMap.containsKey(recipientId)) {
                        GetUserDto profile = userProfilesMap.get(recipientId);
                        boolean isOnline = onlineStatuses.getOrDefault(recipientId, false);
                        profile.setOnline(isOnline);
                        dto.setRecipient(profile);
                    }

                    return dto;
                })
                .sorted(Comparator.comparing(ChatDto::isPinned).reversed())
                .toList();
    }

    public ChatDto findChatById(long chatId, int userId,String token) {
        Chat chat = chatsRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchUsersChatException("Чат с заданным id не найден"));
        var chatDto = ChatMapper.toDto(chat);
        Optional<Message> messageOptional = messagesRepository.findLastMessageByChatIdAndUserId(chat.getId(), userId);
        if (messageOptional.isPresent()) {
            Message message = messageOptional.get();
            setLastMessageToChatDto(message, chatDto);
        }
        if(!chat.isGroup()){
            Integer recipientId = participantsRepository.findRecipientByUserIdAndChatId(userId,chatId)
                    .orElseThrow(()->new NoSuchParticipantException("Участник чата не найден"));
            var recipient = authRestClient.findUserById(recipientId,token);
            chatDto.setRecipient(recipient);
        }
        return chatDto;
    }

    @Transactional
    public ChatDto findPrivateChat(int senderId, int receiverId, String token) {
        Optional<Long> existingChatId = chatsRepository.findChatIdByParticipantsIdForPrivateChats(senderId, receiverId);
        if (existingChatId.isPresent()) {
            return findChatById(existingChatId.get(), senderId, token);
        }
        return createPrivateChat(senderId, receiverId);
    }

    @Transactional
    public ChatDto createPrivateChat(int senderId, int receiverId) {
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

    private void setLastMessageToChatDto(Message message, ChatDto chatDto) {
        if (!Objects.isNull(message)) {
            MessageDto lastMessageDto = messageMapper.toMessageDto(message);
            chatDto.setLastMessage(lastMessageDto);
        }
    }


    @Transactional
    public ChatDto createGroup(CreateGroupDto createGroupDto, int creatorId, String token) {
        Chat chat = ChatMapper.createGroupDtoToEntity(createGroupDto);
        Chat savedChat = chatsRepository.save(chat);
        List<Participants> participants = createGroupDto.membersIds()
                .stream()
                .filter(id -> id != creatorId)
                .map(id -> createParticipantForGroup(ChatRole.MEMBER, id, savedChat))
                .collect(Collectors.toList());
        participants.add(createParticipantForGroup(ChatRole.OWNER, creatorId, savedChat));
        participantsRepository.saveAll(participants);

        var actor = authRestClient.findUserById(creatorId, token);
        MessageDto messageDto = messagesService.saveServiceMessage(new CreateGroupServiceMessage(actor.getUsername(), chat.getName()), (int) chat.getId(), creatorId);
        ChatDto chatDto = ChatMapper.toDto(savedChat);
        chatDto.setLastMessage(messageDto);

        kafkaSenderService.sendMessage(messageDto);
        kafkaSenderService.sendNotification(CreateNotificationRequest
                .builder()
                .title("Вы были добавлены в группу %s".formatted(chat.getName()))
                .users(createGroupDto.membersIds())
                .metadata(Map.of("chatId", chat.getId(), "userId", creatorId))
                .notificationType(NotificationType.INVITE)
                .build());
        return chatDto;
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


    @Transactional
    public void deleteChatById(long chatId, int userId) {
        Participants participants = participantsRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new NoSuchParticipantException("Комбинация чата и его участника не найдена"));
        participants.setDeletedByUser(true);
        participantsRepository.save(participants);
        deletedMessagesRepository.markAllMessagesAsRemovedWhenChatRemoving(chatId, userId);
    }

    public GetUserDto findRecipientIdByChatId(int chatId, int senderId, String token) {
        var userId = chatsRepository.findRecipientIdByChatId(chatId, senderId)
                .orElseThrow(() -> new NoSuchUserException("Участник чата не найден"));

        GetUserDto user = authRestClient.findUserById(userId, token);

        var onlineMap = connectionsServiceRestClient.findUserOnlineStatus(new CheckOnlineRequest(List.of(userId)));
        boolean online = onlineMap.getOrDefault(userId, false);
        user.setOnline(online);
        return user;
    }

    public GroupAccessDto getUserRightsByGroupId(long groupId, int userId) {
        Participants participants = participantsRepository.findByChatIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NoSuchParticipantException("Не найдена связь между чатом и пользователем"));
        return SecurityUtils.determinateGroupAccess(participants);
    }

}
