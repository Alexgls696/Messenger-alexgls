package com.alexgls.springboot.notificationsservice.client;

import java.util.List;

public interface MessageStorageServiceClient {
    List<Integer> findAllParticipantsByChatId(int chatId, String token);
}
