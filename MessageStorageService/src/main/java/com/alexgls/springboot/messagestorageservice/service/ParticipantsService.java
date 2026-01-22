package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.client.AuthWebClient;
import com.alexgls.springboot.messagestorageservice.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservice.entity.ChatRole;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUserException;
import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservice.util.SecurityUtils;
import com.alexgls.springboot.messagestorageservice.util.groups.RemoveUserServiceMessage;
import com.alexgls.springboot.messagestorageservice.util.groups.ServiceMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantsService {

    private final MessagesService messagesService;

    private final KafkaSenderService kafkaSenderService;

    private final ParticipantsRepository participantsRepository;

    private final AuthWebClient authWebClient;

    public Mono<List<GetUserDto>> findAllByChatId(int chatId, String token, int currentUserId) {
        return participantsRepository.findByChatId(chatId)
                .flatMap(participant -> authWebClient
                        .findUserById(participant.getUserId(), token)
                        .map(userDto -> new GetUserDto(userDto.id(), userDto.name(), userDto.surname(), userDto.username(), ChatRole.getTranslate(participant.getRole()))))
                .collectList()
                .map(unsorted -> sortUsersList(unsorted, currentUserId));
    }

    public Flux<Integer> findUserIdsByChatId(int chatId) {
        return participantsRepository.findUserIdsByChatId(chatId);
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

    public Mono<Void> deleteParticipantFromGroup(int chatId, int removingUserId, int actorId, String token) {
        if (removingUserId == actorId) {
            return Mono.error(new AccessDeniedException("У вас нет доступа для выполнения этой операции."));
        }
        return participantsRepository.findByChatIdAndUserId(chatId, actorId)
                .switchIfEmpty(Mono.error(() -> new NoSuchParticipantException("Не найдена связь между чатом и пользователем")))
                .flatMap(participant -> {
                    boolean canRemoveMembers = SecurityUtils.determinateGroupAccess(participant.getRole()).canRemoveMembers();
                    if (!canRemoveMembers) {
                        return Mono.error(new AccessDeniedException("У вас нет доступа на выполнение этой операции"));
                    }
                    if (participant.getRole() == ChatRole.OWNER) {
                        return participantsRepository.findByChatIdAndUserId(chatId, removingUserId)
                                .flatMap(userParticipant -> participantsRepository.removingUserFromGroupByChatIdAndUserId(chatId, removingUserId));
                    }
                    if (participant.getRole() == ChatRole.ADMIN) {
                        return participantsRepository.findByChatIdAndUserId(chatId, removingUserId)
                                .flatMap(removingParticipant -> {
                                    ChatRole role = removingParticipant.getRole();
                                    if (role == ChatRole.OWNER || role == ChatRole.ADMIN) {
                                        return Mono.error(new AccessDeniedException(
                                                "У вас нет доступа на выполнение этой операции"
                                        ));
                                    }
                                    return participantsRepository.removingUserFromGroupByChatIdAndUserId(chatId, removingUserId);
                                });
                    }
                    return Mono.error(new AccessDeniedException("У вас нет доступа на выполнение этой операции"));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(generateRemovingMessageContent(removingUserId, actorId, token))
                .flatMap(serviceMessage -> messagesService.saveServiceMessage(serviceMessage, chatId, actorId))
                .flatMap(savedMessage -> {
                    kafkaSenderService.sendMessage(savedMessage);
                    return Mono.empty();
                });
    }

    private Mono<ServiceMessage> generateRemovingMessageContent(int removingUserId, int actorId, String token) {
        Mono<GetUserDto> removingUserMono = authWebClient.findUserById(removingUserId, token)
                .switchIfEmpty(Mono.error(() -> new NoSuchUserException("Пользователь не найден")));
        Mono<GetUserDto> actorUserMono = authWebClient.findUserById(actorId, token)
                .switchIfEmpty(Mono.error(() -> new NoSuchUserException("Пользователь не найден")));
        return Mono.zip(removingUserMono, actorUserMono)
                .map(tuple -> {
                    var removingUser = tuple.getT1();
                    var actor = tuple.getT2();
                    return new RemoveUserServiceMessage(removingUser.username(), actor.username());
                });
    }

    public Mono<Void> leaveGroup(int chatId, int userId) {
        return participantsRepository.findByChatIdAndUserId(chatId, userId)
                .switchIfEmpty(Mono.error(() -> new NoSuchParticipantException("Не найдена связь между чатом и пользователем")))
                .flatMap(participant -> participantsRepository.leavingFromGroupByChatIdAndUserId(chatId, userId));
    }




}
