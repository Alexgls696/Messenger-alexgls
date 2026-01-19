package com.alexgls.springboot.messagestorageservice.dto;

import com.alexgls.springboot.messagestorageservice.entity.Chat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Column;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ChatWithUnread extends Chat {

    @Column("unread_count") 
    private int unreadCount;
}
