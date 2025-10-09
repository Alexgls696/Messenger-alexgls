package com.alexgls.springboot.userdetailsservice.dto;

import java.util.Date;
import java.util.List;

public record UserProfileResponse(
        int userId,
        Date birthday,
        String status,
        List<Integer> userImagesIds,
        Integer avatarId

) {
}
