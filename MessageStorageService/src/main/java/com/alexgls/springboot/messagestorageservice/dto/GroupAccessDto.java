package com.alexgls.springboot.messagestorageservice.dto;

public record GroupAccessDto(
        boolean canEdit,
        boolean canRemoveMembers,
        boolean canRemoveMessages
) {
}
