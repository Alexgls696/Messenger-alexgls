package com.alexgls.springboot.messagestorageservicevt.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private long id;

    @Column(name = "chat_id")
    private int chatId;

    @Column(name = "sender_id")
    private int senderId;

    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType type;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_read")
    private boolean isRead;

    @Column(name = "read_at")
    private Timestamp readAt;

    @Column(name = "is_service")
    private boolean isService;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "forward_from_user_id")
    private Integer forwardFromUserId;

    @Column(name = "is_forwarded", nullable = false)
    private boolean forwarded;

    @Transient
    private int recipientId;

    @Transient
    private Set<String> tokenHashes;

    @Transient
    private List<Attachment> attachments;
}
