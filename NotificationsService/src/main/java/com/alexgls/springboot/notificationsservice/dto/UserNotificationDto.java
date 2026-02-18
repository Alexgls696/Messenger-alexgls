package com.alexgls.springboot.notificationsservice.dto;

import java.sql.Timestamp;

public record UserNotificationDto(
        int userId,
        long notificationId,
        boolean read,
        Timestamp readAt
) {
}
