package com.alexgls.springboot.metadatastorageservice.dto;

public record FindInChatRequest(
        int chatId,
        String query
) {
}
