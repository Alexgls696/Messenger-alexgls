package com.alexgls.springboot.userdetailsservice.repository;

import com.alexgls.springboot.userdetailsservice.entity.UserImage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UserImagesRepository extends ReactiveCrudRepository<UserImage,Integer> {
    @Query("select ui.image_id from user_images ui where user_id = :userId")
    Flux<Integer> findAllByUserId(@Param("userId") Integer userId);
}
