package com.alexgls.springboot.messagestorageservice.dto;

public record ReadMessagePayload(
        long messageId,
        int senderId,
        int chatId
) {

}
