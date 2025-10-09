package com.alexgls.springboot.userdetailsservice.dto;

import java.util.Date;

public record UserDetailsResponse(
        int userId,
        Date birthday,
        String status
) {
}
