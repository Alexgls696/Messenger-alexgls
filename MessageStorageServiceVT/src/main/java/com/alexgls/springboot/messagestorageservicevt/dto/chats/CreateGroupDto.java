package com.alexgls.springboot.messagestorageservicevt.dto.chats;

import java.util.List;

public record CreateGroupDto(
        String name,
        String description,
        List<Integer> membersIds

) {
}
