package com.alexgls.springboot.messagestorageservicevt.dto.messages;

public record ReplyMessageContent(
        long messageId,
        int senderId,
        String content
) {
}
