package com.alexgls.springboot.userdetailsservice.dto;

public record GetUserDto(
        int id,
        String name,
        String surname,
        String username
) {
}