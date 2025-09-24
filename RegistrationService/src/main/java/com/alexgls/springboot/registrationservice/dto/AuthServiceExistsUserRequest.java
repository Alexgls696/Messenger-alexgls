package com.alexgls.springboot.registrationservice.dto;

public record AuthServiceExistsUserRequest(
        String username,
        String email
) {
}
