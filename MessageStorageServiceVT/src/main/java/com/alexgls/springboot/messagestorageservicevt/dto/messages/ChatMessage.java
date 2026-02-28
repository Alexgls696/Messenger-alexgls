package com.alexgls.springboot.messagestorageservicevt.dto.messages;

import com.alexgls.springboot.messagestorageservicevt.dto.attachments.CreateAttachmentPayload;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatMessage {
    private String chatId;
    private String content;
    private List<CreateAttachmentPayload> attachments;
    private String tempId;
    private Long replyMessageId;
}
