package ru.alexgls.springboot.usersmessagingservice.dto.messages;

public record ReplyMessageContent(
        long messageId,
        int senderId,
        String content
) {
}
