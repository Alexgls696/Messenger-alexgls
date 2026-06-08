package com.alexgls.springboot.metadatastorageservice.dto;

public record SavedMetadataNotificationMessage(
        int chatId,
        int fileId,
        String title,
        String summary
) {
}
