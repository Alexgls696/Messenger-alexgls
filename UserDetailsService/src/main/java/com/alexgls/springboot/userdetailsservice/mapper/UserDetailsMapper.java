package com.alexgls.springboot.userdetailsservice.mapper;

import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.entity.UserDetails;
import com.alexgls.springboot.userdetailsservice.dto.UserDetailsResponse;

public class UserDetailsMapper {
    public static UserDetails toEntity(final UpdateUserDetailsRequest userDetails, int userId) {
        return new UserDetails(0, userId, userDetails.birthday(), userDetails.status());
    }

    public static UserDetailsResponse toDto(final UserDetails userDetails) {
        return new UserDetailsResponse(userDetails.getUserId(), userDetails.getBirthday(), userDetails.getStatus());
    }
}
