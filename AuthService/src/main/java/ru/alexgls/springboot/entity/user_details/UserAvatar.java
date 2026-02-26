package ru.alexgls.springboot.entity.user_details;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_avatars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserAvatar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Integer userImageId;

    private int userId;
}
