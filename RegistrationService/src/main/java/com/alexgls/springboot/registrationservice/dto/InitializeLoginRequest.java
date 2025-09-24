package com.alexgls.springboot.registrationservice.dto;

public record InitializeLoginRequest(
        String username,
        String email,
        String phoneNumber
) {

}
