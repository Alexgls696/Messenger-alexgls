package com.alexgls.springboot.registrationservice.dto;

public record AuthServiceJwtResponse(
        String accessToken,
        String refreshToken
) {

}