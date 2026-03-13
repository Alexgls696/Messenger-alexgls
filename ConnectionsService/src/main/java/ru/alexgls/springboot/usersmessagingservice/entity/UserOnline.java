package ru.alexgls.springboot.usersmessagingservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;


@RedisHash(value = "user:presence", timeToLive = 60L)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserOnline {

    @Id
    private int userId;

    private boolean online;

}
