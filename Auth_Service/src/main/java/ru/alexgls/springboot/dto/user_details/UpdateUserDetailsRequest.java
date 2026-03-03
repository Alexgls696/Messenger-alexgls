package ru.alexgls.springboot.dto.user_details;

import java.time.LocalDate;

public record UpdateUserDetailsRequest(
        LocalDate birthday,
        String status
) {

}
