package com.alexgls.springboot.messagestorageservicevt.dto.messages;

import java.util.List;

public record DeleteMessageRequest(
        List<Long> messagesId,
        int senderId,
        int chatId,
        boolean forAll
) {
}
