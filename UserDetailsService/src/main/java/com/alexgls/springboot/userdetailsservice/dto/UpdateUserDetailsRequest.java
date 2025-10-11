package com.alexgls.springboot.userdetailsservice.dto;

import java.time.LocalDate;

public record UpdateUserDetailsRequest(
        LocalDate birthday,
        String status
) {

}
