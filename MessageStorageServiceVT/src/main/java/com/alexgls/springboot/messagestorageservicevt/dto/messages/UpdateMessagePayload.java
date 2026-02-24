package com.alexgls.springboot.messagestorageservicevt.dto.messages;

public record UpdateMessagePayload(
        long id,
        int chatId,
        String content
) {
}
