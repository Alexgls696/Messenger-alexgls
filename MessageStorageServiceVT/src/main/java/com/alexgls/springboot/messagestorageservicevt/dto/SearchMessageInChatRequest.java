package com.alexgls.springboot.messagestorageservicevt.dto;

public record SearchMessageInChatRequest(
        int chatId,
        String content
) {
}
