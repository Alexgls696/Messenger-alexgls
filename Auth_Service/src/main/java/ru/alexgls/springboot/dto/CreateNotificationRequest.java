package ru.alexgls.springboot.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record CreateNotificationRequest(
        String title,
        String content,
        Integer imageId,
        NotificationType notificationType,
        List<Integer> users,
        Map<String,Object> metadata
) {
}
