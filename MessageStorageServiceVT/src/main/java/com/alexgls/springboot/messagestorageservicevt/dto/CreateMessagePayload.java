package com.alexgls.springboot.messagestorageservicevt.dto;

import java.util.List;

public record CreateMessagePayload(
        Integer chatId,
        int senderId,
        String content,
        List<CreateAttachmentPayload> attachments,
        String tempId
) {}