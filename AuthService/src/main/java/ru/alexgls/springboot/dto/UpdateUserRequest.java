package ru.alexgls.springboot.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank
        String name,
        String surname,

        @NotBlank
        String username
) {
}
