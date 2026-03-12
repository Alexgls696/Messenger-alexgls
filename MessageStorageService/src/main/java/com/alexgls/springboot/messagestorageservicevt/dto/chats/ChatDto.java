package com.alexgls.springboot.messagestorageservicevt.dto.chats;


import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.MessageDto;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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

    private boolean pinned;

    private GetUserDto recipient;

}
