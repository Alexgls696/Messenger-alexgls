package com.alexgls.springboot.searchdataservice.client;

import com.alexgls.springboot.searchdataservice.dto.GetUserDto;

import java.util.List;

public interface AuthServiceRestClient {
    Iterable<GetUserDto> findAllByKey(String key, String token);

    List<GetUserDto> findAllByIds(List<Integer> ids, String token);
}
