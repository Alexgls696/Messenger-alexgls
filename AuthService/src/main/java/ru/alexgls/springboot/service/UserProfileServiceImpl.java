package ru.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.alexgls.springboot.dto.user_details.UpdateUserDetailsRequest;
import ru.alexgls.springboot.dto.user_details.UserProfileResponse;
import ru.alexgls.springboot.entity.user_details.UserAvatar;
import ru.alexgls.springboot.entity.user_details.UserDetails;
import ru.alexgls.springboot.entity.user_details.UserImage;
import ru.alexgls.springboot.exceptions.NoSuchUserDetailsException;
import ru.alexgls.springboot.exceptions.NoSuchUserImageException;
import ru.alexgls.springboot.repository.UserAvatarsRepository;
import ru.alexgls.springboot.repository.UserDetailsRepository;
import ru.alexgls.springboot.repository.UserImagesRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserDetailsRepository userDetailsRepository;
    private final UserAvatarsRepository userAvatarsRepository;
    private final UserImagesRepository userImagesRepository;

    @Override
    public UserProfileResponse findUserProfileByUserId(int userId) {
        UserDetails details = userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchUserDetailsException("Информация профиля пользователя не найдена"));
        List<UserImage> images = userImagesRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Integer userAvatarImageId = userAvatarsRepository.findUserAvatarImageIdByUserId(userId)
                .orElse(0);
        return new UserProfileResponse(userId, details.getBirthday(), details.getStatus(), images, userAvatarImageId);
    }

    @Override
    public void createProfileForUserByUserId(int userId) {
        boolean exists = userDetailsRepository.existsByUserId(userId);
        if (!exists) {
            userDetailsRepository.save(new UserDetails(0, userId, null, null));
        }
    }

    @Override
    public void updateUserDetails(UpdateUserDetailsRequest updateUserDetailsRequest, int userId) {
        UserDetails details = userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchUserDetailsException("Информация профиля пользователя не найдена"));
        details.setStatus(updateUserDetailsRequest.status());
        details.setBirthday(updateUserDetailsRequest.birthday());
        userDetailsRepository.save(details);
    }

    @Transactional
    @Override
    public void deleteImageFromUserProfile(int userImageId, int userId) {
        UserImage userImage = userImagesRepository.findByImageIdAndUserId(userImageId, userId)
                .orElseThrow(() -> new NoSuchUserImageException("Изображение с id %d не найдено".formatted(userImageId)));
        changeUserAvatarWhenImageDelete(userImage.getId(), userId);
        userImagesRepository.deleteUserImageByImageIdAndUserId(userImageId, userId);
    }

    @Override
    public Integer findUserAvatarImageId(int userId) {
        return userAvatarsRepository.findUserAvatarImageIdByUserId(userId)
                .orElse(0);
    }

    @Transactional
    @Override
    public void addImageToUserProfile(int imageId, int userId) {
        UserImage savedUserImage = userImagesRepository.save(new UserImage(0, userId, imageId, Timestamp.from(Instant.now())));
        saveOrUpdateUserAvatar(imageId, userId);

        Optional<UserAvatar> avatar = userAvatarsRepository.findByUserId(userId);
        if (avatar.isPresent()) {
            UserAvatar userAvatar = avatar.get();
            userAvatar.setUserImageId(savedUserImage.getId());
            userAvatarsRepository.save(userAvatar);
        } else {
            userAvatarsRepository.save(new UserAvatar(0, savedUserImage.getId(), userId));
        }
    }

    @Transactional
    protected void saveOrUpdateUserAvatar(int userImageId, int userId) {

    }


    @Transactional
    protected void changeUserAvatarWhenImageDelete(int userImageId, int userId) {
        Optional<UserAvatar> userAvatar = userAvatarsRepository.findByUserImageIdAndUserId(userImageId, userId);
        if (userAvatar.isPresent()) {
            List<UserImage> images = userImagesRepository.findTwoLastImagesByUserIdOrderByCreatedAtDesc(userId);
            if (images.size() > 1) {
                int nextUserImageId = images.get(1).getId();
                userAvatarsRepository.updateUserAvatarByUserId(userId, nextUserImageId);
            } else {
                userAvatarsRepository.deleteUserAvatarByUserId(userId);
            }
        }
    }
}
