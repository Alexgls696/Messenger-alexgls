package ru.alexgls.springboot.usersmessagingservice.dto.notifications;

import java.sql.Timestamp;
import java.util.Map;

public record NotificationDto(
        long id,
        String title,
        String content,
        String type,
        Integer imageId,
        Timestamp createdAt,
        Map<String,Object> metadata,
        boolean read,
        Iterable<UserNotificationDto> recipients
) {
}
