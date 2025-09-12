package com.alexgls.springboot.messagestorageservice.dto;

import java.util.List;

public record DeleteMessageRequest(
        List<Long> messagesId,
        int senderId,
        int chatId,
        boolean forAll
) {
}
