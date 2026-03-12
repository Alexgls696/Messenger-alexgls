package com.alexgls.springboot.messagestorageservicevt.mapper;


import com.alexgls.springboot.messagestorageservicevt.dto.messages.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.MessageDto;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageType;

import java.sql.Timestamp;
import java.time.Instant;

public class MessageMapper {

    public static MessageDto toMessageDto(Message message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setId(message.getId());
        messageDto.setType(message.getType());
        messageDto.setCreatedAt(message.getCreatedAt());
        messageDto.setUpdatedAt(message.getUpdatedAt());
        messageDto.setAttachments(message.getAttachments());
        messageDto.setContent(message.getContent());
        messageDto.setRead(message.isRead());
        messageDto.setSenderId(message.getSenderId());
        messageDto.setRecipientId(message.getRecipientId());
        messageDto.setReadAt(message.getReadAt());
        messageDto.setChatId(message.getChatId());
        messageDto.setService(message.isService());
        messageDto.setReplyToId(message.getReplyToMessageId());
        messageDto.setForwarded(message.isForwarded());
        messageDto.setForwardFromUserId(message.getForwardFromUserId());
        return messageDto;
    }

    public static Message toMessageFromCreateMessagePayload(CreateMessagePayload payload) {
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
        message.setReplyToMessageId(payload.replyMessageId());
        boolean isService = payload.tempId().equals("service");
        message.setService(isService);
        return message;
    }


}
