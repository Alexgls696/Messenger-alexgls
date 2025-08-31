package com.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProxyService {

    private final WebClient webClient;

    private final StorageService storageService;

    private static final Set<String> WHITELISTED_HEADERS = Set.of(
            HttpHeaders.CONTENT_TYPE.toLowerCase(),
            HttpHeaders.CONTENT_LENGTH.toLowerCase(),
            HttpHeaders.CONTENT_DISPOSITION.toLowerCase(),
            HttpHeaders.ETAG.toLowerCase(),
            HttpHeaders.LAST_MODIFIED.toLowerCase(),
            HttpHeaders.CACHE_CONTROL.toLowerCase(),
            HttpHeaders.EXPIRES.toLowerCase()
    );

    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadAndStreamFileById(Integer id) {
        return storageService.getDownloadPathById(id)
                .flatMap(this::streamFileFromUrl);
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadAndStreamFileByPath(String path) {
        return storageService.getDownLoadFilePath(path)
                .flatMap(this::streamFileFromUrl);
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> streamFileFromUrl(String url) {
        String browserUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

        return webClient
                .get()
                .uri(URI.create(url))
                .header(HttpHeaders.USER_AGENT, browserUserAgent)
                .retrieve()
                .toEntityFlux(DataBuffer.class)
                .map(finalResponseEntity -> {

                    HttpHeaders headersForClient = new HttpHeaders();

                    finalResponseEntity.getHeaders().forEach((name, values) -> {
                        if (WHITELISTED_HEADERS.contains(name.toLowerCase())) {
                            headersForClient.addAll(name, values);
                        }
                    });

                    if (!headersForClient.containsKey(HttpHeaders.CONTENT_TYPE)) {
                        headersForClient.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    }

                    return ResponseEntity
                            .status(finalResponseEntity.getStatusCode())
                            .headers(headersForClient)
                            .body(finalResponseEntity.getBody());
                });
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> buildProxyResponse(ClientResponse clientResponse) {
        if (clientResponse.statusCode().isError()) {
            log.warn("Remote server returned an error: {}", clientResponse.statusCode());
            return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
        }

        HttpHeaders responseHeaders = new HttpHeaders();

        clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
            if (WHITELISTED_HEADERS.contains(name.toLowerCase())) {
                responseHeaders.addAll(name, values);
            }
        });

        // Если по какой-то причине Content-Type не пришел, ставим заглушку
        if (!responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        return Mono.just(ResponseEntity
                .status(clientResponse.statusCode())
                .headers(responseHeaders) // Используем наши отфильтрованные заголовки
                .body(clientResponse.bodyToFlux(DataBuffer.class)));
    }
}
