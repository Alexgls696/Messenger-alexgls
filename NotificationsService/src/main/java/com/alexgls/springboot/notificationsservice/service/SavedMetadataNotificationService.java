package com.alexgls.springboot.notificationsservice.service;

import com.alexgls.springboot.notificationsservice.client.AuthServiceClient;
import com.alexgls.springboot.notificationsservice.client.MessageStorageServiceClient;
import com.alexgls.springboot.notificationsservice.config.ServiceTokenWrapper;
import com.alexgls.springboot.notificationsservice.dto.CreateNotificationRequest;
import com.alexgls.springboot.notificationsservice.dto.SavedMetadataNotificationMessage;
import com.alexgls.springboot.notificationsservice.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedMetadataNotificationService {

    private final NotificationsService notificationsService;

    private final AuthServiceClient authServiceClient;

    private final MessageStorageServiceClient messageStorageServiceClient;

    private final ServiceTokenWrapper tokenWrapper;

    public void sendNotification(SavedMetadataNotificationMessage savedMetadataNotificationMessage) {
        Map<String, Object> metadata = Map.of("chatId", savedMetadataNotificationMessage.chatId(),
                "fileId", savedMetadataNotificationMessage.fileId());

        String token = tokenWrapper.getValidToken(authServiceClient::getServiceAccessToken);
        List<Integer> recipients = messageStorageServiceClient.findAllParticipantsByChatId(savedMetadataNotificationMessage.chatId(), token);

        CreateNotificationRequest createNotificationRequest = CreateNotificationRequest
                .builder()
                .notificationType(NotificationType.MESSAGE)
                .title("Анализ файла успешно завершен!")
                .metadata(metadata)
                .users(recipients)
                .content("Файл \"%s\" был успешно проанализирован, краткое описание: \n%s \nОн доступен в чате ".formatted(savedMetadataNotificationMessage.title(),
                        savedMetadataNotificationMessage.summary()))
                .build();

        notificationsService.save(createNotificationRequest);
    }
}
