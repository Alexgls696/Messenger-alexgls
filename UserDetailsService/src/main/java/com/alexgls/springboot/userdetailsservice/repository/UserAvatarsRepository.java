package com.alexgls.springboot.userdetailsservice.repository;

import com.alexgls.springboot.userdetailsservice.entity.UserAvatar;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserAvatarsRepository extends ReactiveCrudRepository<UserAvatar, Integer> {

    @Query("select ui.image_id from user_avatars ua join user_images ui on ua.user_image_id = ui.id where user_id = :userId")
    Mono<Integer> findByUserId(@Param("userId") int userId);
}
