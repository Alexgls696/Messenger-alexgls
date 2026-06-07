package com.alexgls.springboot.notificationsservice.dto;

public record ServiceLoginResponse(String accessToken, String tokenType, Long expiresIn) {}