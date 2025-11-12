package com.alexgls.springboot.searchdataservice.client;

import com.alexgls.springboot.searchdataservice.dto.MessageDto;
import com.alexgls.springboot.searchdataservice.dto.SearchMessageInChatRequest;

import java.util.List;

public interface MessageStorageServiceClient {
    List<MessageDto> findMessagesByContent(SearchMessageInChatRequest searchMessageInChatRequest, String token);
}
