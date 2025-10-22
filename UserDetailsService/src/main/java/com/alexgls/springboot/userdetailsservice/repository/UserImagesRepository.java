package com.alexgls.springboot.userdetailsservice.repository;

import com.alexgls.springboot.userdetailsservice.entity.UserImage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserImagesRepository extends ReactiveCrudRepository<UserImage, Integer> {
    Flux<UserImage> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    Mono<Void> deleteUserImageByImageIdAndUserId(int id, int userId);

    @Query("select * from user_images where user_id = :userId order by created_at desc limit 2")
    Flux<UserImage> findTwoLastImagesByUserIdOrderByCreatedAtDesc(@Param("userId") int userId);

    Mono<UserImage> findByImageIdAndUserId(int id, int userId);
}
