package ru.alexgls.springboot.dto.user_details;

public record GetUserDto(
        int id,
        String name,
        String surname,
        String username
) {
}