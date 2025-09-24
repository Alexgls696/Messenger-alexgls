package com.alexgls.springboot.registrationservice.dto;

public record UserRegisterDto(
        String name,
        String surname,
        String username,
        String password,
        String email
) {
}