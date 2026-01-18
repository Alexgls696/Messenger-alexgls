package com.alexgls.springboot.messagestorageservice.dto;

import java.util.List;

public record CreateGroupDto(
        String name,
        String description,
        List<Integer> membersIds

) {
}
