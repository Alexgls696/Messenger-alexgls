package ru.alexgls.springboot.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthUtil {
    public static Integer getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }
}
