package ru.alexgls.springboot.usersmessagingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import ru.alexgls.springboot.usersmessagingservice.client.MessagesStorageServiceRestClient;
import ru.alexgls.springboot.usersmessagingservice.dto.ToUserOnlineDto;
import ru.alexgls.springboot.usersmessagingservice.dto.UserOnlineDto;
import ru.alexgls.springboot.usersmessagingservice.entity.UserOnline;
import ru.alexgls.springboot.usersmessagingservice.repository.UserOnlineRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final UserOnlineRepository userOnlineRepository;

    private final KafkaTemplate<String, UserOnlineDto> userOnlineKafkaTemplate;

    private final SimpMessagingTemplate messagingTemplate;

    private final MessagesStorageServiceRestClient messagesStorageServiceRestClient;


    public void setOnline(int userId, String token) {
        boolean wasOnline = userOnlineRepository.existsById(userId);
        var userOnline = userOnlineRepository.save(new UserOnline(userId, true));
        if (!wasOnline) {
            notifyOnline(userId, token);
        }
    }

    private void notifyOnline(int userId, String token) {
        UserOnlineDto dto = new UserOnlineDto(userId, true);
        Iterable<Integer> participantsIds =
                messagesStorageServiceRestClient.findAllUsersWhoHadChatWithUser(token);
        for (Integer participantId : participantsIds) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(participantId),
                    "/queue/online-changed",
                    new ToUserOnlineDto(dto.userId(), dto.online(), null)
            );
        }
    }

    public void setOffline(int userId, String token) {
        userOnlineRepository.delete(new UserOnline(userId, false));
        UserOnlineDto userOnlineDto = new UserOnlineDto(userId, false);
        userOnlineKafkaTemplate.send("user-online-status-topic", userOnlineDto);
        Iterable<Integer> participantsIds = messagesStorageServiceRestClient.findAllUsersWhoHadChatWithUser(token);
        for (Integer participantId : participantsIds) {
            messagingTemplate.convertAndSendToUser(String.valueOf(participantId), "/queue/online-changed",
                    new ToUserOnlineDto(userOnlineDto.userId(), userOnlineDto.online(), new Date()));
        }
    }

    public Map<Integer, Boolean> checkOnlineByList(List<Integer> userIds) {
        Iterable<UserOnline> usersOnlineIterable = userOnlineRepository.findAllById(userIds);
        List<UserOnline> userOnlineList = new ArrayList<>();
        for (var user : usersOnlineIterable) {
            userOnlineList.add(user);
        }
        return userOnlineList.stream()
                .collect(Collectors.toMap(UserOnline::getUserId, UserOnline::isOnline));
    }


}
