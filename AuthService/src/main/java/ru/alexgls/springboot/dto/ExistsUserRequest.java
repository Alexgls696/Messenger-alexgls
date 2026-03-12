package ru.alexgls.springboot.dto;

public record ExistsUserRequest(
        String username,
        String email
) {
}
