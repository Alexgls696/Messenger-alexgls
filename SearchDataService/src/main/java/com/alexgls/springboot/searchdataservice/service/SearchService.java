package com.alexgls.springboot.searchdataservice.service;

import com.alexgls.springboot.searchdataservice.dto.GetUserDto;
import com.alexgls.springboot.searchdataservice.dto.MessageDto;
import com.alexgls.springboot.searchdataservice.dto.SearchMessageInChatRequest;

public interface SearchService {
    Iterable<GetUserDto>findAllUsersByUsername(String username, String token);

    Iterable<MessageDto>findMessagesByContentInChat(SearchMessageInChatRequest searchMessageInChatRequest, String token);
}
