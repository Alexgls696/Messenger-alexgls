package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.client.AuthWebClient;
import com.alexgls.springboot.messagestorageservice.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservice.entity.ChatRole;
import com.alexgls.springboot.messagestorageservice.entity.Participants;
import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantsService {
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


}
