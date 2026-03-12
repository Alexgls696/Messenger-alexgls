package ru.alexgls.springboot.usersmessagingservice.dto;

import java.util.List;

public record DeleteMessageResponseToUser(
        List<Long> messagesId,
        int senderId,
        int chatId
) {
}
