package com.alexgls.springboot.searchdataservice.client;

import com.alexgls.springboot.searchdataservice.dto.GetUserDto;

public interface AuthServiceRestClient {
    Iterable<GetUserDto>findAllByUsername(String username, String token);
}
