package ru.alexgls.springboot.dto;

import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class JwtValidationResponse {
    private boolean valid;
    private String message;
    private String userId;
    private List<String> roles;
}
