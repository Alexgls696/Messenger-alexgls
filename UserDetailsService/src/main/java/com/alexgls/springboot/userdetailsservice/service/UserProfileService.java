package com.alexgls.springboot.userdetailsservice.service;

import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.dto.UserDetailsResponse;
import com.alexgls.springboot.userdetailsservice.dto.UserProfileResponse;
import reactor.core.publisher.Mono;

public interface UserProfileService {
    Mono<UserProfileResponse>findUserProfileByUserId(int userId);
    Mono<UserProfileResponse>createProfileForUserByUserId(int userId);
    Mono<UserDetailsResponse>updateUserDetails(UpdateUserDetailsRequest updateUserDetailsRequest);


}
