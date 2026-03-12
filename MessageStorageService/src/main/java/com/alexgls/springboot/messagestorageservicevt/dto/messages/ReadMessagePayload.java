package com.alexgls.springboot.messagestorageservicevt.dto.messages;

public record ReadMessagePayload(
        long messageId,
        int senderId,
        int chatId
) {

}
