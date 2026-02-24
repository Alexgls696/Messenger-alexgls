package com.alexgls.springboot.messagestorageservicevt.dto.messages;

public record SearchMessageInChatRequest(
        int chatId,
        String content
) {
}
