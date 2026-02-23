package com.alexgls.springboot.messagestorageservicevt.dto;


import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@Builder
@ToString
public class ChatDto {
    private long chatId;

    private String name;

    private String description;

    private boolean group;

    private String type;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private MessageDto lastMessage;

    private Integer numberOfUnreadMessages;

}
