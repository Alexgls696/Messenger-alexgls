package com.alexgls.springboot.messagestorageservicevt.mapper;


import com.alexgls.springboot.messagestorageservicevt.dto.attachments.CreateAttachmentPayload;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.ChatMessage;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.MessageDto;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.ReplyMessageContent;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageType;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.getSenderId;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final EncryptUtils encryptUtils;

    public MessageDto toMessageDto(Message message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setId(message.getId());
        messageDto.setType(message.getType());
        messageDto.setCreatedAt(message.getCreatedAt());
        messageDto.setUpdatedAt(message.getUpdatedAt());
        messageDto.setAttachments(message.getAttachments());
        messageDto.setContent(encryptUtils.decrypt(message.getContent()));
        messageDto.setRead(message.isRead());
        messageDto.setSenderId(message.getSenderId());
        messageDto.setRecipientId(message.getRecipientId());
        messageDto.setReadAt(message.getReadAt());
        messageDto.setChatId(message.getChatId());
        messageDto.setService(message.isService());
        messageDto.setForwarded(message.isForwarded());
        messageDto.setForwardFromUserId(message.getForwardFromUserId());
        if(!Objects.isNull(message.getReplyToMessage())){
            messageDto.setReplyMessageContent(new ReplyMessageContent(message.getReplyToMessage().getId(), message.getReplyToMessage().getSenderId(), encryptUtils.decrypt(message.getReplyToMessage().getContent())));
        }
        return messageDto;
    }

    public Message toMessageFromCreateMessagePayload(CreateMessagePayload payload) {
        Message message = new Message();
        message.setCreatedAt(Timestamp.from(Instant.now()));
        message.setContent(payload.content());
        message.setType(
                (payload.attachments() == null || payload.attachments().isEmpty())
                        ? MessageType.TEXT
                        : MessageType.FILE
        );
        message.setSenderId(payload.senderId());
        message.setChatId(payload.chatId());
        boolean isService = payload.tempId().equals("service");
        message.setService(isService);
        return message;
    }

    public CreateMessagePayload getCreateMessagePayload(ChatMessage message, Authentication authentication) {
        Integer senderId = getSenderId(authentication);
        List<CreateAttachmentPayload> attachments = message.getAttachments() != null ? message.getAttachments() : Collections.emptyList();

        return new CreateMessagePayload(
                message.getChatId(),
                senderId,
                message.getContent(),
                attachments,
                message.getTempId(),
                message.getReplyMessageId()
        );
    }


}
