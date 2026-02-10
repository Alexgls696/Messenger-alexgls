package com.alexgls.springboot.messagestorageservicevt.kafka;


import com.alexgls.springboot.messagestorageservicevt.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservicevt.dto.MessageDto;
import com.alexgls.springboot.messagestorageservicevt.service.MessagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.alexgls.springboot.messagestorageservicevt.service.KafkaSenderService;

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
        MessageDto savedMessageDto = messagingService.save(createMessagePayload);
        log.info("Message has been successfully saved in database: {}", savedMessageDto);
        kafkaSenderService.sendMessage(savedMessageDto);
    }

}
