package ru.alexgls.springboot.usersmessagingservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;


@RedisHash(value = "user:presence")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserOnline {

    @Id
    private int userId;

    private boolean online;

    @TimeToLive
    private long expiration;

}
