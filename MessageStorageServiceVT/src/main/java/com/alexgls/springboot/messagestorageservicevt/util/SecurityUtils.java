package com.alexgls.springboot.messagestorageservicevt.util;


import com.alexgls.springboot.messagestorageservicevt.dto.GroupAccessDto;
import com.alexgls.springboot.messagestorageservicevt.entity.ChatRole;
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
        if (chatRole == ChatRole.MODERATOR || chatRole.equals(ChatRole.MEMBER)) {
            return new GroupAccessDto(false, false, true);
        }
        return new GroupAccessDto(false, true, false);
    }
}
