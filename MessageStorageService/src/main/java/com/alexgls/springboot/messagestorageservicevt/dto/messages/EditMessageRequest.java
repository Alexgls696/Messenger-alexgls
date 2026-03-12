package com.alexgls.springboot.messagestorageservicevt.dto.messages;

public record EditMessageRequest(
        long chatId,
        String content
) {
}
