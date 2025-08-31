package com.alexgls.springboot.client;

import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.dto.CreateFileMetadataRequest;
import reactor.core.publisher.Mono;

public interface InDatabaseStorageServiceRestClient {
    Mono<ChatImage> findChatImageById(int id);

    Mono<ChatImage> saveChatImage(CreateFileMetadataRequest createFileMetadataRequest);
}
