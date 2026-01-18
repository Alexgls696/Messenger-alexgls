package com.alexgls.springboot.messagestorageservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Chat {
    @Id
    @Column(value = "chat_id")
    private int chatId;

    @Column(value = "name")
    private String name;

    @Column(value = "description")
    private String description;

    @Column(value = "is_group")
    private boolean isGroup;

    @Column(value = "type")
    private String type;

    @Column(value = "created_at")
    private Timestamp createdAt;

    @Column(value = "updated_at")
    private Timestamp updatedAt;

    private Long lastMessageId;

}
