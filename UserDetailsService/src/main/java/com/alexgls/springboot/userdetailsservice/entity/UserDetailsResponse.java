package com.alexgls.springboot.userdetailsservice.entity;

import java.util.Date;

public record UserDetailsResponse(
        int userId,
        Date birthday,
        String status
) {
}
