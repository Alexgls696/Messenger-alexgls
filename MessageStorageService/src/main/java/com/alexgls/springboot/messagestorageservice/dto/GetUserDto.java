package com.alexgls.springboot.messagestorageservice.dto;

public record GetUserDto(
        int id,
        String name,
        String surname,
        String username,
        String role
) {
}
