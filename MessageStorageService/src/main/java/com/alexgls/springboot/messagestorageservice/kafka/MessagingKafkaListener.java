package com.alexgls.springboot.messagestorageservice.kafka;

import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.service.KafkaSenderService;
import com.alexgls.springboot.messagestorageservice.service.MessagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessagingKafkaListener {

    private final MessagesService messagingService;

    private final KafkaSenderService kafkaSenderService;

    @KafkaListener(topics = "messaging-topic", groupId = "messaging-group", containerFactory = "kafkaCreateMessageListenerContainerFactory")
    public void listen(CreateMessagePayload createMessagePayload) {
        log.info("Message received: {}", createMessagePayload);
        log.info("Starting message saving process in database...");
        messagingService.save(createMessagePayload)
                .doOnNext(savedMessageDto -> {
                    log.info("Message has been successfully saved in database: {}", savedMessageDto);
                    kafkaSenderService.sendMessage(savedMessageDto);
                })
                .subscribe(
                        result -> log.info("Reactive stream completed successfully for payload: {}", createMessagePayload),
                        error -> log.error(" FAILED to process message payload: " + createMessagePayload, error)
                );
    }

}
