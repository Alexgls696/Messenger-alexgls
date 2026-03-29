package com.alexgls.springboot.messagestorageservicevt.dto.messages;

import lombok.Builder;

@Builder
public record ReadMessagePayload(
        long messageId,
        int senderId,
        int chatId
) {

}
