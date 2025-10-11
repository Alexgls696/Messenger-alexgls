package com.alexgls.springboot.userdetailsservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "user_avatars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserAvatar {
    @Id
    private int id;

    @Column("user_image_id")
    private Integer userImageId;

    private int userId;
}
