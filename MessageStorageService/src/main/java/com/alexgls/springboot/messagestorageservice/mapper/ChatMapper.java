package com.alexgls.springboot.messagestorageservice.mapper;

import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.dto.CreateGroupDto;
import com.alexgls.springboot.messagestorageservice.entity.Chat;

import java.sql.Timestamp;
import java.time.Instant;

public class ChatMapper {

    public static ChatDto toDto(Chat chat) {
        ChatDto chatDto = new ChatDto();
        chatDto.setName(chat.getName());
        chatDto.setType(chat.getType());
        chatDto.setGroup(chat.isGroup());
        chatDto.setCreatedAt(chat.getCreatedAt());
        chatDto.setUpdatedAt(chat.getUpdatedAt());
        chatDto.setChatId(chat.getChatId());
        chatDto.setDescription(chat.getDescription());
        return chatDto;
    }

    public static Chat createGroupDtoToEntity(CreateGroupDto createGroupDto) {
        Chat chat = new Chat();
        chat.setCreatedAt(Timestamp.from(Instant.now()));
        chat.setUpdatedAt(Timestamp.from(Instant.now()));
        chat.setType("GROUP");
        chat.setGroup(true);
        chat.setName(createGroupDto.name());
        chat.setDescription(createGroupDto.description());
        return chat;
    }
}
