package com.alexgls.springboot.userdetailsservice.service;

import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.dto.UserDetailsResponse;
import com.alexgls.springboot.userdetailsservice.dto.UserProfileResponse;
import com.alexgls.springboot.userdetailsservice.entity.UserAvatar;
import com.alexgls.springboot.userdetailsservice.entity.UserDetails;
import com.alexgls.springboot.userdetailsservice.entity.UserImage;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserDetailsException;
import com.alexgls.springboot.userdetailsservice.mapper.UserDetailsMapper;
import com.alexgls.springboot.userdetailsservice.repository.UserAvatarsRepository;
import com.alexgls.springboot.userdetailsservice.repository.UserDetailsRepository;
import com.alexgls.springboot.userdetailsservice.repository.UserImagesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserDetailsRepository userDetailsRepository;
    private final UserAvatarsRepository userAvatarsRepository;
    private final UserImagesRepository userImagesRepository;

    @Override
    public Mono<UserProfileResponse> findUserProfileByUserId(int userId) {
        Mono<UserDetails> userDetailsMono = userDetailsRepository.findById(userId);
        Flux<Integer> userImagesFlux = userImagesRepository.findAllByUserId(userId);
        Mono<Integer> userAvatarImageIdMono = userAvatarsRepository.findByUserId(userId).defaultIfEmpty(-1);

        return Mono.zip(userDetailsMono, userImagesFlux.collectList(), userAvatarImageIdMono)
                .flatMap(tuple -> {
                    UserDetails userDetails = tuple.getT1();
                    List<Integer> imagesIds = tuple.getT2();
                    Integer userAvatarImageId = tuple.getT3();
                    if (userAvatarImageId == -1) {
                        userAvatarImageId = null;
                    }
                    return Mono.just(new UserProfileResponse(userId, userDetails.getBirthday(), userDetails.getStatus(), imagesIds, userAvatarImageId));
                });
    }

    @Override
    public Mono<UserProfileResponse> createProfileForUserByUserId(int userId) {
        return userDetailsRepository.save(new UserDetails(0, userId, null, null))
                .map(userDetails -> new UserProfileResponse(userDetails.getId(), userDetails.getBirthday(), userDetails.getStatus(), null, null));
    }

    @Override
    public Mono<UserDetailsResponse> updateUserDetails(UpdateUserDetailsRequest updateUserDetailsRequest) {
        return userDetailsRepository.findByUserId(updateUserDetailsRequest.userId())
                .flatMap(userDetails -> {
                    userDetails.setStatus(updateUserDetailsRequest.status());
                    userDetails.setBirthday(updateUserDetailsRequest.birthday());
                    return userDetailsRepository.save(userDetails);
                }).flatMap(updated -> Mono.just(UserDetailsMapper.toDto(updated)))
                .switchIfEmpty(Mono.error(() -> new NoSuchUserDetailsException("User details for user with id %d not found".formatted(updateUserDetailsRequest.userId()))));
    }
}
