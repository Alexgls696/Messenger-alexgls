package ru.alexgls.springboot.dto.user_details;

import java.time.LocalDate;


public record UserDetailsResponse(
        int userId,
        LocalDate birthday,
        String status
) {
}
