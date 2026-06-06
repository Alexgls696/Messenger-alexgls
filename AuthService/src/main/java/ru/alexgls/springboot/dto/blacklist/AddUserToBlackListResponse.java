package ru.alexgls.springboot.dto.blacklist;

public record AddUserToBlackListResponse(
        int userId,
        int blockedUserId,
        String message
) {
}
