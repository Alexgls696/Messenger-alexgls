package com.alexgls.springboot.indatabasecontentstorageservice.util;


import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {

    public static Integer getSenderId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }

    public static String getToken(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getTokenValue();
    }

}
