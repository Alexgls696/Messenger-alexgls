package ru.alexgls.springboot.usersmessagingservice.dto;

public record ReadMessagePayload(
        int messageId,
        int senderId,
        int chatId
) {

}