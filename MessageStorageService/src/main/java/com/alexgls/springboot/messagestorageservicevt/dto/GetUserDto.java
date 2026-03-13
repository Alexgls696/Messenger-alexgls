package com.alexgls.springboot.messagestorageservicevt.dto;

import lombok.*;

import java.util.Date;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUserDto {
    private int id;
    private String name;
    private String surname;
    private String username;
    private String role;
    private Date lastSeenAt;
    private Boolean online;
}
