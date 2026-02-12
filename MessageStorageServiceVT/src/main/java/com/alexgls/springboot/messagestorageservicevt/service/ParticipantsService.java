package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.entity.ChatRole;
import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUserException;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils;
import com.alexgls.springboot.messagestorageservicevt.util.groups.RemoveUserServiceMessage;
import com.alexgls.springboot.messagestorageservicevt.util.groups.ServiceMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantsService {

    private final MessagesService messagesService;

    private final KafkaSenderService kafkaSenderService;

    private final ParticipantsRepository participantsRepository;

    private final AuthRestClient authRestClient;

    public List<GetUserDto> findAllByChatId(int chatId, String token, int currentUserId) {
        var participants = participantsRepository.findAllByChatId(chatId);
        Map<Integer, Participants> participantsMap = participants.stream()
                .collect(Collectors.toMap(Participants::getUserId, (participant -> participant)));
        List<GetUserDto> unsortedUsers = authRestClient.findAllUsers(participantsMap.keySet(), token)
                .stream()
                .map(user -> new GetUserDto(user.id(), user.name(), user.surname(), user.username(), ChatRole.getTranslate(participantsMap.get(user.id()).getRole())))
                .toList();
        return sortUsersList(unsortedUsers, currentUserId);
    }

    private List<GetUserDto> sortUsersList(List<GetUserDto> unsorted, int currentUserId) {
        var sorted = unsorted.stream()
                .sorted(Comparator.comparing(GetUserDto::name))
                .collect(Collectors.toList());
        int meIndex = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).id() == currentUserId) {
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

    public List<Integer> findUserIdsByChatId(int chatId) {
        return participantsRepository.findUserIdsByChatId(chatId);
    }

    @Transactional
    public void deleteParticipantFromGroup(int chatId, int removingUserId, int actorId, String token) {
        if (removingUserId == actorId) {
            throw new AccessDeniedException("У вас нет доступа для выполнения этой операции.");
        }
        Participants participant = participantsRepository.findByChatIdAndUserId(chatId, actorId)
                .orElseThrow(() -> new NoSuchParticipantException("Не найдена связь между чатом и пользователем"));
        boolean canRemoveMembers = SecurityUtils.determinateGroupAccess(participant.getRole()).canRemoveMembers();
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
        participantsRepository.removingUserFromGroupByChatIdAndUserId(chatId, removingUserId);
    }

    protected ServiceMessage generateRemovingMessageContent(int removingUserId, int actorId, String token) {
        GetUserDto removingUser = authRestClient.findUserById(removingUserId, token);
        GetUserDto actor = authRestClient.findUserById(actorId, token);

        if (removingUser == null || actor == null) {
            throw new NoSuchUserException("Пользователь не найден");
        }
        return new RemoveUserServiceMessage(removingUser.username(), actor.username());
    }

    public void leaveGroup(long chatId, int userId) {
        boolean exists = participantsRepository.existsByChatIdAndUserId(chatId, userId);
        if (!exists) {
            throw new NoSuchParticipantException("Не найдена связь между чатом и пользователем");
        }
        participantsRepository.leavingFromGroupByChatIdAndUserId(chatId, userId);
    }

}
