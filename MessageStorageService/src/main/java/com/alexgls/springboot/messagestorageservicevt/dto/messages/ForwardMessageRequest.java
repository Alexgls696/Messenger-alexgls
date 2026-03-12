package com.alexgls.springboot.messagestorageservicevt.dto.messages;

import java.util.List;

public record ForwardMessageRequest(
        ChatMessage chatMessage,
        List<Long> forwardedMessagesIds
) {
}
