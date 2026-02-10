package com.alexgls.springboot.messagestorageservicevt.dto;

import com.alexgls.springboot.messagestorageservicevt.entity.Chat;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


public record ChatWithUnread(Chat chat, int unreadCount) {}
