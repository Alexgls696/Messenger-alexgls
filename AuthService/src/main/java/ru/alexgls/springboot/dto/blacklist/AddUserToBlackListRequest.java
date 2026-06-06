package ru.alexgls.springboot.dto.blacklist;

public record AddUserToBlackListRequest(
        int userId,
        int blockedUserId
) {
}
