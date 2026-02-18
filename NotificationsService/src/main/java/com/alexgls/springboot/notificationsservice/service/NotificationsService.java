package com.alexgls.springboot.notificationsservice.service;

import com.alexgls.springboot.notificationsservice.dto.CreateNotificationRequest;
import com.alexgls.springboot.notificationsservice.dto.NotificationDto;
import com.alexgls.springboot.notificationsservice.dto.UserNotificationDto;
import com.alexgls.springboot.notificationsservice.entity.*;
import com.alexgls.springboot.notificationsservice.repository.NotificationsRepository;
import com.alexgls.springboot.notificationsservice.repository.UserNotificationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class NotificationsService {

    private final NotificationsRepository notificationsRepository;

    private final UserNotificationsRepository userNotificationsRepository;

    private final KafkaSender kafkaSender;

    public List<NotificationDto> findAllByUserId(int userId, Pageable pageable) {
        return notificationsRepository.findAllByUserId(userId, pageable)
                .stream()
                .map(notification -> {
                    boolean isRead = notification.getUserNotifications()
                            .stream()
                            .anyMatch(un->un.getId().getUserId() == userId && un.isRead());
                    return NotificationDto.builder()
                            .id(notification.getId())
                            .title(notification.getTitle())
                            .content(notification.getContent())
                            .type(NotificationType.DEFAULT.toString())
                            .metadata(notification.getMetadata())
                            .read(isRead)
                            .createdAt(notification.getCreatedAt())
                            .imageId(notification.getImageId())
                            .build();
                }).toList();

    }

    List<UserNotificationDto> notificationDtos(Iterable<UserNotification> userNotifications) {
        Iterable<UserNotification> savedNotifications = userNotificationsRepository.saveAll(userNotifications);
        List<UserNotificationDto> userNotificationDtos = new ArrayList<>();
        for (UserNotification userNotification : savedNotifications) {
            userNotificationDtos.add(new UserNotificationDto(userNotification.getId().getUserId(),
                    userNotification.getId().getNotificationId(),
                    userNotification.isRead(),
                    userNotification.getReadAt()));
        }
        return userNotificationDtos;
    }


    @Transactional
    public NotificationDto save(CreateNotificationRequest createNotificationRequest) {
        Notification notification = Notification.builder()
                .title(createNotificationRequest.title())
                .content(createNotificationRequest.content())
                .type(createNotificationRequest.notificationType() == null ? NotificationType.DEFAULT : createNotificationRequest.notificationType())
                .metadata(createNotificationRequest.metadata())
                .imageId(createNotificationRequest.imageId())
                .createdAt(Timestamp.from(Instant.now()))
                .build();

        final Notification saved = notificationsRepository.save(notification);

        List<UserNotification> userNotifications = createNotificationRequest.users()
                .stream()
                .map(userId -> new UserNotification(new UserNotificationId(saved.getId(), userId), false, null, notification))
                .toList();
        Iterable<UserNotification> savedUserNotification = userNotificationsRepository.saveAll(userNotifications);
        var userNotificationDtos = notificationDtos(savedUserNotification);


        var notificationDto = NotificationDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .content(saved.getContent())
                .imageId(saved.getImageId())
                .read(false)
                .type(saved.getType().toString())
                .recipients(userNotificationDtos)
                .createdAt(saved.getCreatedAt())
                .metadata(saved.getMetadata())
                .build();

        CompletableFuture.runAsync(() -> kafkaSender.sendNotification(notificationDto));
        return notificationDto;
    }

    public Integer findUnreadCountForUser(int userId) {
        return userNotificationsRepository.findAllByUserIdWhereUnread(userId);
    }

    @Transactional
    public void readAllNotifications(int userId) {
        userNotificationsRepository.readAllByUserIdWhereUnread(userId);
    }

    @Transactional
    public void readOneNotification(int notificationId, int userId) {
        userNotificationsRepository.readByNotificationId(notificationId, userId);
    }
}
