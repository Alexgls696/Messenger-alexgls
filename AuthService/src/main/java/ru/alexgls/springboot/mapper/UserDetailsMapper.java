package ru.alexgls.springboot.mapper;


import ru.alexgls.springboot.dto.user_details.UpdateUserDetailsRequest;
import ru.alexgls.springboot.dto.user_details.UserDetailsResponse;
import ru.alexgls.springboot.entity.user_details.UserDetails;

public class UserDetailsMapper {
    public static UserDetails toEntity(final UpdateUserDetailsRequest userDetails, int userId) {
        return new UserDetails(0, userId, userDetails.birthday(), userDetails.status());
    }

    public static UserDetailsResponse toDto(final UserDetails userDetails) {
        return new UserDetailsResponse(userDetails.getUserId(), userDetails.getBirthday(), userDetails.getStatus());
    }
}
