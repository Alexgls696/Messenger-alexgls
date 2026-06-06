package ru.alexgls.springboot.dto.blacklist;

public record DeleteUserFromBlackListResponse(
        int userId,
        int unblockedUserId,
        String message
) {
}
