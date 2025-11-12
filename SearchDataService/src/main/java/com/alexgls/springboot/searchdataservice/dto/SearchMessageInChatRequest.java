package com.alexgls.springboot.searchdataservice.dto;

public record SearchMessageInChatRequest(
        int chatId,
        String content
) {
}
