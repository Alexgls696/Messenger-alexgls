package ru.alexgls.springboot.dto;


import java.util.Date;

public record GetUserDto(
        int id,
        String name,
        String surname,
        String username,
        Date lastSeenAt
) {
}
