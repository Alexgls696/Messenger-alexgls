package ru.alexgls.springboot.dto;


public record GetUserDto(
        int id,
        String name,
        String surname,
        String username
) {
}
