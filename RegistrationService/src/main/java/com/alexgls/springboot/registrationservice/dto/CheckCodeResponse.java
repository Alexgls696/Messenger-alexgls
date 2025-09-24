package com.alexgls.springboot.registrationservice.dto;

public record CheckCodeResponse(
        String id,
        String accessToken,
        String refreshToken
) {
}
