package ru.alexgls.springboot.usersmessagingservice.dto;

import java.util.List;

public record CheckOnlineRequest(
        List<Integer> usersIds
) {
}
