package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.client.ConnectionsServiceRestClient;
import com.alexgls.springboot.messagestorageservicevt.dto.CheckOnlineRequest;
import com.alexgls.springboot.messagestorageservicevt.dto.chats.AddParticipantsToGroupDto;
import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.dto.chats.GroupParticipantsDto;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.MessageDto;
import com.alexgls.springboot.messagestorageservicevt.dto.notifications.CreateNotificationRequest;
import com.alexgls.springboot.messagestorageservicevt.dto.notifications.NotificationType;
import com.alexgls.springboot.messagestorageservicevt.entity.Chat;
import com.alexgls.springboot.messagestorageservicevt.entity.ChatRole;
import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUserException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservicevt.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils;
import com.alexgls.springboot.messagestorageservicevt.util.groups.InviteGroupServiceMessage;
import com.alexgls.springboot.messagestorageservicevt.util.groups.LeaveUserServiceMessage;
import com.alexgls.springboot.messagestorageservicevt.util.groups.RemoveUserServiceMessage;
import com.alexgls.springboot.messagestorageservicevt.util.groups.ServiceMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantsService {

    private final MessagesService messagesService;

    private final KafkaSenderService kafkaSenderService;

    private final ParticipantsRepository participantsRepository;

    private final ChatsRepository chatsRepository;

    private final AuthRestClient authRestClient;

    private final ConnectionsServiceRestClient connectionsServiceRestClient;

    public GroupParticipantsDto findAllByChatId(int chatId, String token, int currentUserId) {
        var participants = participantsRepository.findAllByChatId(chatId);
        Map<Integer, Participants> participantsMap = participants.stream()
                .collect(Collectors.toMap(Participants::getUserId, (participant -> participant)));
        var currentUser = participantsMap.get(currentUserId);
        boolean removed = Objects.isNull(currentUser);
        List<GetUserDto> unsortedUsers = authRestClient.findAllUsers(participantsMap.keySet(), token)
                .stream()
                .peek(user -> user.setRole(ChatRole.getTranslate(participantsMap.get(user.getId()).getRole())))
                .toList();
        var sortedUsers = sortUsersList(unsortedUsers, currentUserId);
        List<Integer> userIds = sortedUsers.stream().map(GetUserDto::getId).toList();
        Map<Integer, Boolean> onlineStatuses = connectionsServiceRestClient.findUserOnlineStatus(new CheckOnlineRequest(userIds));

        for (GetUserDto user : sortedUsers) {
            if (onlineStatuses.containsKey(user.getId())) {
                boolean online = onlineStatuses.get(user.getId());
                if (online) {
                    user.setOnline(true);
                }
            }
        }
        return new GroupParticipantsDto(sortedUsers, removed);
    }

    private List<GetUserDto> sortUsersList(List<GetUserDto> unsorted, int currentUserId) {
        var sorted = unsorted.stream()
                .sorted(Comparator.comparing(GetUserDto::getName))
                .collect(Collectors.toList());
        int meIndex = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId() == currentUserId) {
                meIndex = i;
                break;
            }
        }
        if (meIndex == -1) {
            return sorted;
        }
        GetUserDto me = sorted.get(meIndex);
        sorted.remove(meIndex);
        sorted.add(0, me);
        return sorted;
    }

    public List<Integer> findUserIdsByChatId(long chatId) {
        return participantsRepository.findUserIdsByChatId(chatId);
    }

    @Transactional
    public void deleteParticipantFromGroup(int chatId, int removingUserId, int actorId, String token) {
        if (removingUserId == actorId) {
            throw new AccessDeniedException("У вас нет доступа для выполнения этой операции.");
        }
        Participants participant = participantsRepository.findByChatIdAndUserId(chatId, actorId)
                .orElseThrow(() -> new NoSuchParticipantException("Не найдена связь между чатом и пользователем"));
        boolean canRemoveMembers = SecurityUtils.determinateGroupAccess(participant).canRemoveMembers();
        if (!canRemoveMembers) {
            throw new AccessDeniedException("У вас нет доступа на выполнение этой операции");
        }
        var serviceMessage = generateRemovingMessageContent(removingUserId, actorId, token);
        var savedMessageDto = messagesService.saveServiceMessage(serviceMessage, chatId, actorId);
        if (participant.getRole() == ChatRole.OWNER) {
            validateAndRemove(chatId, removingUserId, false);
        } else {
            validateAndRemove(chatId, removingUserId, true);
        }
        kafkaSenderService.sendMessage(savedMessageDto);
    }

    @Transactional
    protected void validateAndRemove(int chatId, int removingUserId, boolean checkAdminRole) {
        Participants removingParticipant = participantsRepository.findByChatIdAndUserId(chatId, removingUserId)
                .orElseThrow(() -> new NoSuchParticipantException("Пользователь не найден в чате"));

        if (checkAdminRole) {
            ChatRole role = removingParticipant.getRole();
            if (role == ChatRole.OWNER || role == ChatRole.ADMIN) {
                throw new AccessDeniedException("У вас нет доступа на выполнение этой операции");
            }
        }
        participantsRepository.removingUserFromGroupByChatIdAndUserId(chatId, removingUserId, Timestamp.from(Instant.now().plus(Duration.ofSeconds(1))));
    }

    protected ServiceMessage generateRemovingMessageContent(int removingUserId, int actorId, String token) {
        GetUserDto removingUser = authRestClient.findUserById(removingUserId, token);
        GetUserDto actor = authRestClient.findUserById(actorId, token);

        if (removingUser == null || actor == null) {
            throw new NoSuchUserException("Пользователь не найден");
        }
        return new RemoveUserServiceMessage(removingUser.getUsername(), actor.getUsername());
    }

    protected ServiceMessage generateLeaveUserMessageContent(int actorId, String token) {
        GetUserDto actor = authRestClient.findUserById(actorId, token);
        return new LeaveUserServiceMessage(actor.getUsername());
    }

    @Transactional
    public void leaveGroup(long chatId, int userId, String token) {
        boolean exists = participantsRepository.existsByChatIdAndUserId(chatId, userId);
        if (!exists) {
            throw new NoSuchParticipantException("Не найдена связь между чатом и пользователем");
        }
        ServiceMessage message = generateLeaveUserMessageContent(userId, token);
        messagesService.saveServiceMessage(message, chatId, userId);
        participantsRepository.leavingFromGroupByChatIdAndUserId(chatId, userId);
    }

    @Transactional
    public void enterGroup(int chatId, int userId){
        Participants participants = participantsRepository.findByChatIdAndUserId(chatId,userId)
                .orElseThrow(()->new NoSuchParticipantException("Не найдена связь между чатом и пользователем"));
        if(participants.isRemoved()){
            throw new AccessDeniedException("У вас нет доступа на выполнение данной операции");
        }
        participants.setLeave(false);
    }


    /**
     * Возвращает id пользователей, с которыми у данного пользователя есть чат.
     *
     * @param userId Id текущего пользователя
     * @return Iterable с id пользователей.
     */
    public Iterable<Integer> findAllUsersWhoHadChatWith(Integer userId) {
        return participantsRepository.findAllUsersWhoHadChatWith(userId);
    }

    @Transactional
    public void addParticipantsToGroup(AddParticipantsToGroupDto addParticipantsToGroupDto, Integer userId, String token) {
        Chat chat = chatsRepository.findById(addParticipantsToGroupDto.chatId())
                .orElseThrow(() -> new NoSuchUsersChatException("Чат для добавления пользователей не найден"));
        Participants participants = participantsRepository.findByChatIdAndUserId(addParticipantsToGroupDto.chatId(), userId)
                .orElseThrow(() -> new NoSuchParticipantException("Вы не состоите в этом чате, действие невозможно"));
        var access = SecurityUtils.determinateGroupAccess(participants);
        if (!access.canRemoveMembers()) {
            throw new AccessDeniedException("У вас нет доступа на выполнение этой операции");
        }

        List<Participants> removedParticipants = participantsRepository.findAllRemovedParticipants(addParticipantsToGroupDto.chatId())
                .stream()
                .filter(p -> addParticipantsToGroupDto.participantsIds().contains(p.getUserId()))
                .peek(p -> {
                    p.setLeave(false);
                    p.setRemoved(false);
                    addParticipantsToGroupDto.participantsIds().remove(p.getUserId());
                })
                .toList();
        participantsRepository.saveAll(removedParticipants);

        List<Participants> newParticipantsList = addParticipantsToGroupDto.participantsIds()
                .stream()
                .map(participantUserId -> Participants.builder()
                        .chat(chat)
                        .leave(false)
                        .isDeletedByUser(false)
                        .joinedAt(Timestamp.from(Instant.now()))
                        .userId(participantUserId)
                        .unreadCount(0)
                        .role(ChatRole.MEMBER)
                        .lastReadMessageId(null)
                        .build())
                .toList();
        participantsRepository.saveAll(newParticipantsList);

        var sendToUsersIds = getToUsersIdsList(removedParticipants, addParticipantsToGroupDto.participantsIds());

        kafkaSenderService.sendNotification(CreateNotificationRequest
                .builder()
                .title("Вы были добавлены в группу \"%s\"".formatted(chat.getName()))
                .users(sendToUsersIds)
                .notificationType(NotificationType.INVITE)
                .metadata(Map.of("chatId", chat.getId(), "userId", userId))
                .build());

        var actor = authRestClient.findUserById(userId, token);
        var invitedUsers = authRestClient.findAllUsers(sendToUsersIds, token);
        for (var user : invitedUsers) {
            MessageDto serviceInviteMessage = messagesService.saveServiceMessage(new InviteGroupServiceMessage(actor.getUsername(), user.getUsername()), (int) chat.getId(), userId);
            kafkaSenderService.sendMessage(serviceInviteMessage);
        }
    }

    private List<Integer> getToUsersIdsList(List<Participants> participants, Set<Integer> ids) {
        var result = new ArrayList<Integer>();
        for (Participants p : participants) {
            result.add(p.getUserId());
        }
        result.addAll(ids);
        return result;
    }

    public boolean existsByChatIdAndUserId(int chatId, int userId) {
        return participantsRepository.existsByChatIdAndUserId(chatId, userId);
    }
}
