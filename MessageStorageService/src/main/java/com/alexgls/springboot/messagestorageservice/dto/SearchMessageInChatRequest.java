package com.alexgls.springboot.messagestorageservice.dto;

public record SearchMessageInChatRequest(
        int chatId,
        String content
) {
}
