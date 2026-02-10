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


    //TODO Оптимизировать, грузить сразу список пользователей из сервиса аутентификации, а не по одному
    public List<GetUserDto> findAllByChatId(int chatId, String token, int currentUserId) {
        var unsortedUsers = participantsRepository.findAllByChatId(chatId)
                .stream()
                .map(participant -> createUserDtoForAllChats(participant, token))
                .toList();
        return sortUsersList(unsortedUsers, currentUserId);
    }

    public GetUserDto createUserDtoForAllChats(Participants participant, String token) {
        var userDto = authRestClient.findUserById(participant.getUserId(), token);
        return new GetUserDto(userDto.id(), userDto.name(), userDto.surname(), userDto.username(), ChatRole.getTranslate(participant.getRole()));
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
                .orElseThrow(() -> new NoSuchParticipantException("Не найдена связь между чатом и пользователем")); // Предполагаем, что репозиторий возвращает Optional

        boolean canRemoveMembers = SecurityUtils.determinateGroupAccess(participant.getRole()).canRemoveMembers();
        if (!canRemoveMembers) {
            throw new AccessDeniedException("У вас нет доступа на выполнение этой операции");
        }

        if (participant.getRole() == ChatRole.OWNER) {
            validateAndRemove(chatId, removingUserId, false);
        } else if (participant.getRole() == ChatRole.ADMIN) {
            validateAndRemove(chatId, removingUserId, true);
        } else {
            throw new AccessDeniedException("У вас нет доступа на выполнение этой операции");
        }

        var serviceMessage = generateRemovingMessageContentAsync(removingUserId, actorId, token);
        var savedMessageDto = messagesService.saveServiceMessage(serviceMessage, chatId, actorId);
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

    private ServiceMessage generateRemovingMessageContentAsync(int removingUserId, int actorId, String token) {
        GetUserDto removingUser = authRestClient.findUserById(removingUserId, token);
        GetUserDto actor = authRestClient.findUserById(actorId, token);

        if (removingUser == null || actor == null) {
            throw new NoSuchUserException("Пользователь не найден");
        }
        return new RemoveUserServiceMessage(removingUser.username(), actor.username());
    }

    public void leaveGroup(int chatId, int userId) {
        boolean exists = participantsRepository.existsByChatIdAndUserId(chatId, userId);
        if (!exists) {
            throw new NoSuchParticipantException("Не найдена связь между чатом и пользователем");
        }
        participantsRepository.leavingFromGroupByChatIdAndUserId(chatId, userId);
    }

}
