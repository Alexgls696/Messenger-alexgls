package ru.alexgls.springboot.usersmessagingservice.dto;

public record CreateAttachmentPayload(
        Long fileId,
        String mimeType,
        String fileName
) {
}