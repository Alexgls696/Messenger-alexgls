package com.alexgls.springboot.messagestorageservice.dto;


public record CreateAttachmentPayload(
        Long fileId,
        String mimeType,
        String fileName
) {
}