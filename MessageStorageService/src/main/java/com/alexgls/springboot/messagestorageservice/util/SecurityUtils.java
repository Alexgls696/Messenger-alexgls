package com.alexgls.springboot.messagestorageservice.util;

import com.alexgls.springboot.messagestorageservice.dto.GroupAccessDto;
import com.alexgls.springboot.messagestorageservice.entity.ChatRole;
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

    public static GroupAccessDto determinateGroupAccess(ChatRole chatRole) {
        if (chatRole == ChatRole.OWNER || chatRole == ChatRole.ADMIN) {
            return new GroupAccessDto(true, true, true);
        }
        if (chatRole == ChatRole.MODERATOR) {
            return new GroupAccessDto(false, false, true);
        }
        return new GroupAccessDto(false, true, false);
    }
}
