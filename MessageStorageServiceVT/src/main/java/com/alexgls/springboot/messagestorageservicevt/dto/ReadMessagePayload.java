package com.alexgls.springboot.messagestorageservicevt.dto;

public record ReadMessagePayload(
        long messageId,
        int senderId,
        int chatId
) {

}
