package ru.alexgls.springboot.usersmessagingservice.dto.notifications;

import java.sql.Timestamp;

public record UserNotificationDto(
        int userId,
        long notificationId,
        boolean read,
        Timestamp readAt
) {
}
