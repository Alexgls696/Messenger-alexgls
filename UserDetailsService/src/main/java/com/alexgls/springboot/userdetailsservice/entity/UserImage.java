package com.alexgls.springboot.userdetailsservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;


@Table(name = "user_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserImage {
    @Id
    private int id;
    private int userId;
    private int imageId;
    private Timestamp createdAt;
}
