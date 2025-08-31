package com.alexgls.springboot.client;

import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.exception.InDatabaseServiceException;
import com.alexgls.springboot.exception.NoSuchImageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class InDatabaseStorageServiceRestClientImpl implements InDatabaseStorageServiceRestClient {

    private final WebClient webClient;

    @Override
    public Mono<ChatImage> findChatImageById(int id) {
        try {
            return webClient
                    .get()
                    .uri("/api/images/{id}", id)
                    .retrieve()
                    .bodyToMono(ChatImage.class);
        } catch (HttpClientErrorException.NotFound exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            String error = (String) problemDetail.getProperties().get("error");
            log.warn("WebClient exception: {}", error);
            throw new NoSuchImageException(error);
        } catch (WebClientResponseException exception) {
            log.warn("In database service client exception: {}", exception.getResponseBodyAsString());
            throw new InDatabaseServiceException(exception.getResponseBodyAsString());
        }
    }

    @Override
    public Mono<ChatImage> saveChatImage(CreateFileMetadataRequest createFileMetadataRequest) {
        try {
            return webClient
                    .post()
                    .uri("/api/images")
                    .bodyValue(createFileMetadataRequest)
                    .retrieve()
                    .bodyToMono(ChatImage.class);
        } catch (WebClientResponseException exception) {
            log.warn("In database service client exception: {}", exception.getResponseBodyAsString());
            throw new InDatabaseServiceException(exception.getResponseBodyAsString());
        }
    }
}
