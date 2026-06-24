package ru.alexgls.springboot.usersmessagingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
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

    private final SimpMessagingTemplate messagingTemplate;

    private final MessagesStorageServiceRestClient messagesStorageServiceRestClient;


    public void setOnline(int userId) {
        boolean wasOnline = userOnlineRepository.existsById(userId);
        var userOnline = userOnlineRepository.save(new UserOnline(userId, true, 60));
        if (!wasOnline) {
            notifyOnline(userId);
        }
    }

    private void notifyOnline(int userId) {
        UserOnlineDto dto = new UserOnlineDto(userId, true);
        Iterable<Integer> participantsIds =
                messagesStorageServiceRestClient.findAllUsersWhoHadChatWithUser(userId);
        for (Integer participantId : participantsIds) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(participantId),
                    "/queue/online-changed",
                    new ToUserOnlineDto(dto.userId(), dto.online(), null)
            );
        }
    }

    public void setOffline(int userId) {
        UserOnline userOnline = userOnlineRepository.findById(userId).orElse(null);
        if(userOnline == null) {
            return;
        }
        userOnline.setExpiration(15);
        userOnlineRepository.save(userOnline);
    }


    public Map<Integer, Boolean> checkOnlineByList(List<Integer> userIds) {
        Iterable<UserOnline> onlineUsers = userOnlineRepository.findAllById(userIds);

        Map<Integer, Boolean> resultMap = new HashMap<>();
        userIds.forEach(id -> resultMap.put(id, false));
        onlineUsers.forEach(user -> resultMap.put(user.getUserId(), true));

        return resultMap;
    }

}
