package ru.alexgls.springboot.usersmessagingservice.dto.notifications;

import lombok.Builder;

import java.sql.Timestamp;
import java.util.Map;

@Builder
public record NotificationToUser(
        long id,
        String title,
        String content,
        String type,
        Integer imageId,
        boolean read,
        Timestamp createdAt,
        Map<String,Object> metadata
) {
}
