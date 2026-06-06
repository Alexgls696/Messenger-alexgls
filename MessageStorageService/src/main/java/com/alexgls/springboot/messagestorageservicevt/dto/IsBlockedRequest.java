package com.alexgls.springboot.messagestorageservicevt.dto;

public record IsBlockedRequest(
        int userId,
        int targetUserId
) {
}
