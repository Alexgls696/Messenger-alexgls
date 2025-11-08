package com.alexgls.springboot.searchdataservice.dto;

public record GetUserDto(
        int id,
        String name,
        String surname,
        String username
) {
}