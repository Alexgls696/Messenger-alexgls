package com.alexgls.springboot.messagestorageservicevt.dto.chats;

import com.alexgls.springboot.messagestorageservicevt.entity.Chat;


public record ChatWithUnread(Chat chat, int unreadCount) {}
