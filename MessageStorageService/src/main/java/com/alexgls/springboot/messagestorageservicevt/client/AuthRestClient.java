package com.alexgls.springboot.messagestorageservicevt.client;


import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.dto.IsBlockedRequest;

import java.util.List;

public interface AuthRestClient {
    GetUserDto findUserById(int id, String token);

    List<GetUserDto> findAllUsers(Iterable<Integer> ids, String token);

    boolean isBlocked(IsBlockedRequest, String token);
}
