package ru.alexgls.springboot.usersmessagingservice.dto.messages;

import ru.alexgls.springboot.usersmessagingservice.dto.CreateAttachmentPayload;

import java.util.List;

public record CreateMessagePayload(
        Integer chatId,
        int senderId,
        String content,
        List<CreateAttachmentPayload> attachments,
        String tempId
) {
}