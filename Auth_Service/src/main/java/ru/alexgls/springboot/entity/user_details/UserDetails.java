package ru.alexgls.springboot.entity.user_details;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDate;

@Entity
@Table(name = "user_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int userId;

    private LocalDate birthday;

    private String status;
}
