package ru.alexgls.springboot.usersmessagingservice.dto.messages;

import lombok.*;
import ru.alexgls.springboot.usersmessagingservice.dto.CreateAttachmentPayload;

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
}
