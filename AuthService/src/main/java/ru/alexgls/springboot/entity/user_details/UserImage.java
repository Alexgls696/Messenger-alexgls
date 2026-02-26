package ru.alexgls.springboot.entity.user_details;

import jakarta.persistence.*;
import lombok.*;


import java.sql.Timestamp;


@Entity
@Table(name = "user_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int userId;
    private int imageId;
    private Timestamp createdAt;
}
