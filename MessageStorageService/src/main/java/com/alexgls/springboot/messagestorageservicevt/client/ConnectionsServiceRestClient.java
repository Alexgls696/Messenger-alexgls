package com.alexgls.springboot.messagestorageservicevt.client;

import com.alexgls.springboot.messagestorageservicevt.dto.CheckOnlineRequest;
import com.alexgls.springboot.messagestorageservicevt.exceptions.ConnectionServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
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
        }
        catch (ResourceAccessException exception) {
            log.error("Connections Service недоступен (ошибка канала): {}", exception.getMessage());
            return Collections.emptyMap();
        }
        catch (HttpClientErrorException exception) {
            log.error("Ошибка API при получении статусов: {}", exception.getResponseBodyAsString());
            return Collections.emptyMap();
        }
        catch (Exception exception) {
            log.error("Непредвиденная ошибка при запросе статусов", exception);
            return Collections.emptyMap();
        }
    }

}
