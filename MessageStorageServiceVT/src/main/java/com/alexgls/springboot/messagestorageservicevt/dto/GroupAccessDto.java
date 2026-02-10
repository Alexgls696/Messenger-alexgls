package com.alexgls.springboot.messagestorageservicevt.dto;

public record GroupAccessDto(
        boolean canEdit,
        boolean canRemoveMembers,
        boolean canRemoveMessages
) {
}
