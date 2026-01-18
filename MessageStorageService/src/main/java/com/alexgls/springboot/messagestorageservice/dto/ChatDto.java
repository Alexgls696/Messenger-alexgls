package com.alexgls.springboot.messagestorageservice.dto;


import com.alexgls.springboot.messagestorageservice.entity.Message;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatDto {
    private int chatId;

    private String name;

    private String description;

    private boolean isGroup;

    private String type;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private MessageDto lastMessage;

    private Integer numberOfUnreadMessages;

}
