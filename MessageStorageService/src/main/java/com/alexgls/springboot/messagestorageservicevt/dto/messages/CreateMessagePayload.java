package com.alexgls.springboot.messagestorageservicevt.dto.messages;

import com.alexgls.springboot.messagestorageservicevt.dto.attachments.CreateAttachmentPayload;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateMessagePayload(
        long chatId,
        int senderId,
        String content,
        List<CreateAttachmentPayload> attachments,
        String tempId,
        Long replyMessageId
) {}