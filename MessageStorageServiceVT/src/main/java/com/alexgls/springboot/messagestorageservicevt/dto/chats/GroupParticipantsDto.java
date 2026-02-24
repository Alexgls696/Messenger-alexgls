package com.alexgls.springboot.messagestorageservicevt.dto.chats;

import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;

import java.util.List;

public record GroupParticipantsDto(
        List<GetUserDto> participants,
        boolean removed
) {
}
