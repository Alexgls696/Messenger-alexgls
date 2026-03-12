package com.alexgls.springboot.messagestorageservicevt.dto.chats;

import java.util.Set;

public record AddParticipantsToGroupDto(
        long chatId,
        Set<Integer> participantsIds
) {
}
