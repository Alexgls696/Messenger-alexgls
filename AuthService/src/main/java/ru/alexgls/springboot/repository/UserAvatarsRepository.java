package ru.alexgls.springboot.repository;


import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import ru.alexgls.springboot.entity.user_details.UserAvatar;

import java.util.Optional;

@Repository
public interface UserAvatarsRepository extends CrudRepository<UserAvatar, Integer> {

    Optional<UserAvatar> findByUserId(@Param("userId") int userId);

    @Query("select id from UserAvatar where userId = :userId and userImageId = :imageId")
    Optional<UserAvatar> findByUserImageIdAndUserId(@Param("imageId") int imageId, @Param("userId") int userId);


    @Query(value = "select ui.image_id from user_images ui " +
            "join user_avatars ua on ui.id = ua.user_image_id " +
            "where ua.user_id = :userId", nativeQuery = true)
    Optional<Integer> findUserAvatarImageIdByUserId(@Param("userId") int userId);

    @Modifying
    @Query(value = "delete from user_avatars where user_id = :userId", nativeQuery = true)
    void deleteUserAvatarByUserId(@Param("userId") int userId);

    @Modifying
    @Query(value = "update user_avatars set user_image_id = :userImageId where user_id = :userId", nativeQuery = true)
    void updateUserAvatarByUserId(@Param("userId") int userId, @Param("userImageId") int userImageId);


}
