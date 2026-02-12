package com.alexgls.springboot.messagestorageservicevt.dto;

import lombok.Builder;

@Builder
public record GetUserDto(
        int id,
        String name,
        String surname,
        String username,
        String role
) {
}
