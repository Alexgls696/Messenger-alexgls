package com.alexgls.springboot.messagestorageservicevt.dto.messages;

public record ReplyMessageRequest(
        long replyMessageId,
        ChatMessage chatMessage
) {

}
