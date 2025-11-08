package com.alexgls.springboot.searchdataservice.service;

import com.alexgls.springboot.searchdataservice.dto.GetUserDto;

public interface SearchService {
    Iterable<GetUserDto>findAllUsersByUsername(String username, String token);
}
