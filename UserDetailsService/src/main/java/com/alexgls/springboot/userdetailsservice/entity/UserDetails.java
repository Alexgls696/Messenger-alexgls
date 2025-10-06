package com.alexgls.springboot.userdetailsservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

@Table(name = "user_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDetails {

    @Id
    private int id;

    private int userId;

    private Date birthday;

    private String status;
}
