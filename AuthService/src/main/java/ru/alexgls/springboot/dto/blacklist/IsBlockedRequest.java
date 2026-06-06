package ru.alexgls.springboot.dto.blacklist;

public record IsBlockedRequest(
        int userId,
        int targetUserId
) {
}
