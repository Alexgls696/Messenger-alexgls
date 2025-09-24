package com.alexgls.springboot.registrationservice.dto;

public record CheckCodeRequest(
        String id,
        String code
) {
}
