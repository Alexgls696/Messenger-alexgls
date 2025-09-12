package com.alexgls.springboot.messagestorageservice.dto;

import java.util.List;

public record DeleteMessageResponse(
        List<Long> messagesId,
        List<Integer>recipientsId,
        int senderId,
        int chatId,
        boolean forAll
) {
}
