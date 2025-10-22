package com.alexgls.springboot.userdetailsservice.service;

import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.dto.UserDetailsResponse;
import com.alexgls.springboot.userdetailsservice.dto.UserProfileResponse;
import reactor.core.publisher.Mono;

public interface UserProfileService {
    Mono<UserProfileResponse> findUserProfileByUserId(int userId);

    Mono<Void> createProfileForUserByUserId(int userId);

    Mono<Void> updateUserDetails(UpdateUserDetailsRequest updateUserDetailsRequest, int userId);

    Mono<Void> addImageToUserProfile(int imageId, int userId);

    Mono<Void> deleteImageFromUserProfile(int userImageId, int userId);

    Mono<Integer> findUserAvatarImageId(int userId);
}
