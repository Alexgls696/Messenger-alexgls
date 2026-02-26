package ru.alexgls.springboot.dto.user_details;



import ru.alexgls.springboot.entity.user_details.UserImage;

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
