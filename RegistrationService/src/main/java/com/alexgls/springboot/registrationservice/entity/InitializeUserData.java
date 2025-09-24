package com.alexgls.springboot.registrationservice.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.RedisHash;

@RedisHash
@Data
@AllArgsConstructor
public class InitializeUserData {
    private String id;
    private String code;
    private String username;
    private String email;
    private String phoneNumber;
}
