package com.alexgls.springboot.userdetailsservice.dto;

import com.alexgls.springboot.userdetailsservice.entity.UserImage;

import java.time.LocalDate;
import java.util.List;

public record UserProfileResponse(
        int userId,
        LocalDate birthday,
        String status,
        List<UserImage> userImages,
        Integer avatarId

) {
}
