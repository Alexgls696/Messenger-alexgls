package com.alexgls.springboot.messagestorageservicevt.dto.messages;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private long id;

    private int chatId;

    private int senderId;

    private int recipientId;

    private List<Integer> recipientIds;

    private String content;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private boolean isRead;

    private Timestamp readAt;

    private MessageType type;

    private List<Attachment> attachments;

    private String tempId;

    private boolean isService;

    private Long replyToId;

    private Integer forwardFromUserId;

    private boolean forwarded;
}
