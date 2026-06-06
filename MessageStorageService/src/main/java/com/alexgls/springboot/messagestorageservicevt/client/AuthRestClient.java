package com.alexgls.springboot.messagestorageservicevt.client;


import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;

import java.util.List;

public interface AuthRestClient {
    GetUserDto findUserById(int id, String token);

    List<GetUserDto> findAllUsers(Iterable<Integer> ids, String token);

    boolean isBlocked(int targetUserId, String token);

    boolean isBlockedChat(int targetUserId, String token);
}
