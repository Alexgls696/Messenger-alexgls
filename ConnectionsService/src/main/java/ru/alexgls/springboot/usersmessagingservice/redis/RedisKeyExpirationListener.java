package ru.alexgls.springboot.usersmessagingservice.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.alexgls.springboot.usersmessagingservice.client.MessagesStorageServiceRestClient;
import ru.alexgls.springboot.usersmessagingservice.dto.ToUserOnlineDto;
import ru.alexgls.springboot.usersmessagingservice.dto.UserOnlineDto;

import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisKeyExpirationListener implements MessageListener {

    private final KafkaTemplate<String, UserOnlineDto> userOnlineKafkaTemplate;

    private final SimpMessagingTemplate messagingTemplate;

    private final MessagesStorageServiceRestClient messagesStorageServiceRestClient;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        log.info("Received key: " + key);
        if(key.contains("user:presence:")){
            int userId = Integer.parseInt(key.replace("user:presence:", ""));
            log.info("Пользователь офлайн: " + userId);
            UserOnlineDto userOnlineDto = new UserOnlineDto(userId, false);
            userOnlineKafkaTemplate.send("user-online-status-topic", userOnlineDto);
            Iterable<Integer> participantsIds = messagesStorageServiceRestClient.findAllUsersWhoHadChatWithUser(userId);
            for (Integer participantId : participantsIds) {
                messagingTemplate.convertAndSendToUser(String.valueOf(participantId), "/queue/online-changed",
                        new ToUserOnlineDto(userOnlineDto.userId(), userOnlineDto.online(), new Date()));
            }
        }
    }
}