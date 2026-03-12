package com.alexgls.springboot.messagestorageservicevt.dto;

import java.util.List;

public record CheckOnlineRequest(
        List<Integer> usersIds
) {
}
