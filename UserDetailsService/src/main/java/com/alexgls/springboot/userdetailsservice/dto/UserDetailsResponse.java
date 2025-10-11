package com.alexgls.springboot.userdetailsservice.dto;

import java.time.LocalDate;


public record UserDetailsResponse(
        int userId,
        LocalDate birthday,
        String status
) {
}
