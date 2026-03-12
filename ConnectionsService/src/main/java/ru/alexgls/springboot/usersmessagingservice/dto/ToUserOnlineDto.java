package ru.alexgls.springboot.usersmessagingservice.dto;

import java.util.Date;

public record ToUserOnlineDto(
        int userId,
        boolean online,
        Date lastSeenAt
) {
}
