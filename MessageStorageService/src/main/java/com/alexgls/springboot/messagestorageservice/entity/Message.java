package com.alexgls.springboot.messagestorageservice.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;
import java.util.List;

@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Message {
    @Id
    @Column(value = "message_id")
    private long id;

    @Column(value = "chat_id")
    private int chatId;

    @Column(value = "sender_id")
    private int senderId;

    private String content;

    @Column(value = "message_type")
    private MessageType type;

    @Column(value = "created_at")
    private Timestamp createdAt;

    @Column(value = "updated_at")
    private Timestamp updatedAt;

    @Column(value = "is_read")
    private boolean isRead;

    @Column(value = "read_at")
    private Timestamp readAt;

    @Column(value = "deleted_for_user_id")
    private int deletedForUserId;

    @Transient
    private int recipientId;

    @Transient
    private List<Attachment> attachments;
}
