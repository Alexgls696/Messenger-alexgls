package com.alexgls.springboot.messagestorageservicevt.client;

import com.alexgls.springboot.messagestorageservicevt.dto.CheckOnlineRequest;
import com.alexgls.springboot.messagestorageservicevt.exceptions.ConnectionServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RequiredArgsConstructor
public class ConnectionsServiceRestClient {

    private final ParameterizedTypeReference<Map<Integer, Boolean>>PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<Map<Integer, Boolean>>() {};

    private final RestClient restClient;


    public Map<Integer, Boolean>findUserOnlineStatus(CheckOnlineRequest checkOnlineRequest){
        try{
            return restClient.post()
                    .uri("/api/online/check-by-list")
                    .body(checkOnlineRequest)
                    .retrieve()
                    .body(PARAMETERIZED_TYPE_REFERENCE);
        }catch (HttpClientErrorException exception){
            throw new ConnectionServiceException("При попытке получить статус онлайн пользователей произошла ошибка... "+exception.getResponseBodyAsString());
        }
    }

}
