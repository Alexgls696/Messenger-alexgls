package com.alexgls.springboot.searchdataservice.service;

import com.alexgls.springboot.searchdataservice.client.AuthServiceRestClient;
import com.alexgls.springboot.searchdataservice.client.MessageStorageServiceClient;
import com.alexgls.springboot.searchdataservice.dto.GetUserDto;
import com.alexgls.springboot.searchdataservice.dto.MessageDto;
import com.alexgls.springboot.searchdataservice.dto.SearchMessageInChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final AuthServiceRestClient usersServiceRestClient;

    private final MessageStorageServiceClient messageStorageServiceClient;

    @Override
    public Iterable<GetUserDto> findAllUsersByUsername(String username, String token) {
        return usersServiceRestClient.findAllByUsername(username, token);
    }

    @Override
    public Iterable<MessageDto> findMessagesByContentInChat(SearchMessageInChatRequest searchMessageInChatRequest, String token) {
        return messageStorageServiceClient.findMessagesByContent(searchMessageInChatRequest, token);
    }

    @Override
    public Iterable<GetUserDto> findAllUsersFromChats(String token) {
        List<Integer> usersIds = messageStorageServiceClient.findAllUsersWhoHadChatWith(token);
        List<GetUserDto> users = usersServiceRestClient.findAllByIds(usersIds, token);
        List<GetUserDto> result = new ArrayList<>();

        for (int i = 0; i < usersIds.size() ; i++) {
            for (int j = 0; j < users.size(); j++) {
                if(users.get(j).id()==usersIds.get(i)) {
                    result.add(users.get(j));
                }
            }
        }
        return result;
    }
}
