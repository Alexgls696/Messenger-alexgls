package com.alexgls.springboot.userdetailsservice.service;

import com.alexgls.springboot.userdetailsservice.dto.UpdateUserDetailsRequest;
import com.alexgls.springboot.userdetailsservice.dto.UserDetailsResponse;
import com.alexgls.springboot.userdetailsservice.dto.UserProfileResponse;
import com.alexgls.springboot.userdetailsservice.entity.UserAvatar;
import com.alexgls.springboot.userdetailsservice.entity.UserDetails;
import com.alexgls.springboot.userdetailsservice.entity.UserImage;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserAvatarException;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserDetailsException;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserImageException;
import com.alexgls.springboot.userdetailsservice.mapper.UserDetailsMapper;
import com.alexgls.springboot.userdetailsservice.repository.UserAvatarsRepository;
import com.alexgls.springboot.userdetailsservice.repository.UserDetailsRepository;
import com.alexgls.springboot.userdetailsservice.repository.UserImagesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserDetailsRepository userDetailsRepository;
    private final UserAvatarsRepository userAvatarsRepository;
    private final UserImagesRepository userImagesRepository;

    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<UserProfileResponse> findUserProfileByUserId(int userId) {
        Mono<UserDetails> userDetailsMono = userDetailsRepository.findByUserId(userId);
        Flux<UserImage> userImagesFlux = userImagesRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Mono<Integer> userAvatarImageIdMono = userAvatarsRepository.findUserAvatarImageIdByUserId(userId)
                .defaultIfEmpty(0);

        return Mono.zip(userDetailsMono, userImagesFlux.collectList(), userAvatarImageIdMono)
                .switchIfEmpty(Mono.error(new NoSuchUserDetailsException("Информация о пользователе с id %d не найдена".formatted(userId))))
                .flatMap(tuple -> {
                    UserDetails userDetails = tuple.getT1();
                    List<UserImage> imagesIds = tuple.getT2();
                    Integer userAvatarImageId = tuple.getT3();
                    return Mono.just(new UserProfileResponse(userId, userDetails.getBirthday(), userDetails.getStatus(), imagesIds, userAvatarImageId));
                });
    }

    @Override
    public Mono<Void> createProfileForUserByUserId(int userId) {
        return userDetailsRepository.existsByUserId(userId)
                .flatMap(exists -> exists ? Mono.empty() : userDetailsRepository.save(new UserDetails(0, userId, null, null)).then());
    }

    @Override
    public Mono<Void> updateUserDetails(UpdateUserDetailsRequest updateUserDetailsRequest, int userId) {
        return userDetailsRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(() -> new NoSuchUserDetailsException("User details for user with id %d not found".formatted(userId))))
                .flatMap(userDetails -> {
                    userDetails.setStatus(updateUserDetailsRequest.status());
                    userDetails.setBirthday(updateUserDetailsRequest.birthday());
                    return userDetailsRepository.save(userDetails).then();
                })
                .as(transactionalOperator::transactional);

    }

    @Override
    public Mono<Void> deleteImageFromUserProfile(int userImageId, int userId) {
        return userImagesRepository.findByImageIdAndUserId(userImageId, userId)
                .switchIfEmpty(Mono.error(() -> new NoSuchUserImageException("Изображение с id %d не найдено".formatted(userImageId))))
                .flatMap(existed -> changeUserAvatarWhenImageDelete(userImageId, userId))
                .then(userImagesRepository.deleteUserImageByImageIdAndUserId(userImageId, userId))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Integer> findUserAvatarImageId(int userId) {
        return userAvatarsRepository.findUserAvatarImageIdByUserId(userId)
                .switchIfEmpty(Mono.error(()-> new NoSuchUserAvatarException("Аватар пользователя не найден")));
    }

    @Override
    public Mono<Void> addImageToUserProfile(int imageId, int userId) {
        return userImagesRepository.save(new UserImage(0, userId, imageId, Timestamp.from(Instant.now())))
                .flatMap(addedImage -> saveOrUpdateUserAvatar(addedImage.getId(), userId))
                .as(transactionalOperator::transactional)
                .then();
    }

    private Mono<Void> saveOrUpdateUserAvatar(int userImageId, int userId) {
        return userAvatarsRepository.findByUserId(userId)
                .switchIfEmpty(userAvatarsRepository.save(new UserAvatar(0, userImageId, userId)))
                .flatMap(avatar -> {
                    avatar.setUserImageId(userImageId);
                    return userAvatarsRepository.save(avatar);
                }).then();
    }


    public Mono<Void> changeUserAvatarWhenImageDelete(int userImageId, int userId) {
        return userAvatarsRepository.findByUserImageIdAndUserId(userImageId, userId)
                .switchIfEmpty(Mono.empty()) //Случай удаления фотографии, которая не является аватаркой
                .flatMap(userAvatar -> userImagesRepository.findTwoLastImagesByUserIdOrderByCreatedAtDesc(userId).collectList())
                .flatMap(list -> {
                    if (list.size() > 1) {
                        int nextUserImageId = list.get(1).getId();
                        return userAvatarsRepository.updateUserAvatarByUserId(userId, nextUserImageId);
                    } else {
                        return userAvatarsRepository.deleteUserAvatarByUserId(userId);
                    }
                });
    }
}
