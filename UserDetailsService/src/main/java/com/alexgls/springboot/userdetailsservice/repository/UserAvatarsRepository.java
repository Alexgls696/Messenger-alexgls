package com.alexgls.springboot.userdetailsservice.repository;

import com.alexgls.springboot.userdetailsservice.entity.UserAvatar;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAvatarsRepository extends ReactiveCrudRepository<UserAvatar, Integer> {

}
