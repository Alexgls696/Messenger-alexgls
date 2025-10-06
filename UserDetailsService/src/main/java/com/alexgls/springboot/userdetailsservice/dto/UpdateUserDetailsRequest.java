package com.alexgls.springboot.userdetailsservice.dto;

import java.util.Date;

public record UpdateUserDetailsRequest(
        int userId,
        Date birthday,
        String status
) {

}
