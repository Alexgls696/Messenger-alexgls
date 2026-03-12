package ru.alexgls.springboot.dto;

public record UserOnlineDto(
        int userId,
        boolean online
) {
}
