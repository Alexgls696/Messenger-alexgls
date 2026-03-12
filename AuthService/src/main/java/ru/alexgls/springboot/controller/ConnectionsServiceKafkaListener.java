package ru.alexgls.springboot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.alexgls.springboot.dto.UserOnlineDto;
import ru.alexgls.springboot.service.UsersService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionsServiceKafkaListener {

    private final UsersService usersService;

    @KafkaListener(topics = "user-online-status-topic", groupId = "user-online", containerFactory = "kafkaListenerContainerFactory")
    public void listen(UserOnlineDto userOnlineDto) {
        log.info("Online status changed: {}", userOnlineDto);
        usersService.setUserLastSeen(userOnlineDto);
    }

}
