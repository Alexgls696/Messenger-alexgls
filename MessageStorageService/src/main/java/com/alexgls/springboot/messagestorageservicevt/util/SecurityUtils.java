package com.alexgls.springboot.messagestorageservicevt.util;


import com.alexgls.springboot.messagestorageservicevt.dto.chats.GroupAccessDto;
import com.alexgls.springboot.messagestorageservicevt.entity.ChatRole;
import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
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

    public static GroupAccessDto determinateGroupAccess(Participants participants) {
        ChatRole chatRole = participants.getRole();
        boolean canEdit = false, canRemoveMembers = false, canRemoveMessages = false;
        if (chatRole == ChatRole.OWNER || chatRole == ChatRole.ADMIN) {
            canEdit = true; canRemoveMembers = true; canRemoveMessages = true;
        }
        if (chatRole == ChatRole.MODERATOR || chatRole.equals(ChatRole.MEMBER)) {
            canEdit = false; canRemoveMembers = false; canRemoveMessages = true;
        }
        return new GroupAccessDto(canEdit,canRemoveMembers, canRemoveMessages, participants.isLeave(), participants.isRemoved());
    }
}
