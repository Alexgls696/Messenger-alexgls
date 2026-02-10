package com.alexgls.springboot.messagestorageservicevt.dto;

public record UpdateMessagePayload(
        long id,
        int chatId,
        String content
) {
}
