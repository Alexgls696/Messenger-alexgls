package ru.alexgls.springboot.dto.blacklist;

public record DeleteUserFromBlackListRequest(
        int userId,
        int unblockedUserId
) {
}
