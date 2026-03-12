package ru.alexgls.springboot.usersmessagingservice.dto;

public record UserOnlineDto(
        int userId,
        boolean online
) {
}
