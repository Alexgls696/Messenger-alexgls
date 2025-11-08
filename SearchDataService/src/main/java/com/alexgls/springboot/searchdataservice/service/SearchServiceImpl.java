package com.alexgls.springboot.searchdataservice.service;

import com.alexgls.springboot.searchdataservice.client.AuthServiceRestClient;
import com.alexgls.springboot.searchdataservice.dto.GetUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final AuthServiceRestClient usersServiceRestClient;

    @Override
    public Iterable<GetUserDto> findAllUsersByUsername(String username, String token) {
        return usersServiceRestClient.findAllByUsername(username, token);
    }
}
