package com.alexgls.springboot.userdetailsservice.repository;

import com.alexgls.springboot.userdetailsservice.entity.UserAvatar;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserAvatarsRepository extends ReactiveCrudRepository<UserAvatar, Integer> {

    @Query("select * from user_avatars where user_id = :userId")
    Mono<UserAvatar> findByUserId(@Param("userId") int userId);

    Mono<UserAvatar> findByUserImageIdAndUserId(int imageId, int userId);

    @Query("select ui.image_id from user_images ui join user_avatars ua on ui.id = ua.user_image_id where ua.user_id = :userId")
    Mono<Integer>findUserAvatarImageIdByUserId(@Param("userId") int userId);

    @Query("delete from user_avatars where user_id = :userId")
    Mono<Void> deleteUserAvatarByUserId(@Param("userId") int userId);

    @Query("update user_avatars set user_image_id = :userImageId where user_id = :userId")
    Mono<Void> updateUserAvatarByUserId(@Param("userId") int userId, @Param("avatar") int userImageId);


}
