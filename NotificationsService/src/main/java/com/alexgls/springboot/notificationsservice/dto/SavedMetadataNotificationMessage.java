package com.alexgls.springboot.notificationsservice.dto;

public record SavedMetadataNotificationMessage(
        int chatId,
        int fileId,
        String title,
        String summary
) {
}
