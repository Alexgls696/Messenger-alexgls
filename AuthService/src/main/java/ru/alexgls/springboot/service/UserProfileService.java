package ru.alexgls.springboot.service;

import ru.alexgls.springboot.dto.user_details.*;

public interface UserProfileService {
    UserProfileResponse findUserProfileByUserId(int userId);

    void createProfileForUserByUserId(int userId);

    void updateUserDetails(UpdateUserDetailsRequest updateUserDetailsRequest, int userId);

    void addImageToUserProfile(int imageId, int userId);

    void deleteImageFromUserProfile(int userImageId, int userId);

    Integer findUserAvatarImageId(int userId);
}
