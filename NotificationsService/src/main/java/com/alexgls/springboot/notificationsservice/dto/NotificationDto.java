package com.alexgls.springboot.notificationsservice.dto;

import lombok.Builder;

import java.sql.Timestamp;
import java.util.Map;

@Builder
public record NotificationDto(
        long id,
        String title,
        String content,
        String type,
        Integer imageId,
        Timestamp createdAt,
        boolean read,
        Map<String,Object> metadata,
        Iterable<UserNotificationDto> recipients
) {
}
