package com.alexgls.springboot.service;

import com.alexgls.springboot.client.InDatabaseStorageServiceRestClient;
import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.util.FilenameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
        return storageService.getFileMetadataById(id)
                .flatMap(fileData -> streamFileFromUrl(fileData.getPath(), fileData.getFilename()));
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadAndStreamFileByPath(String path) {
        return storageService.getDownLoadFilePath(path)
                .flatMap(resultPath -> streamFileFromUrl(resultPath, null));
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> streamFileFromUrl(String url, final String filename) {
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
                    if (!Objects.isNull(filename)) {
                        String contentDisposition = createContentDispositionHeader(filename);
                        headersForClient.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
                    }
                    if (!headersForClient.containsKey(HttpHeaders.CONTENT_TYPE)) {
                        headersForClient.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    }
                    return ResponseEntity
                            .status(finalResponseEntity.getStatusCode())
                            .headers(headersForClient)
                            .body(finalResponseEntity.getBody());
                });
    }

    private String createContentDispositionHeader(String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        String asciiFallback = filename
                .replaceAll("[^A-Za-z0-9.\\-]", "_")
                .replaceAll("_+", "_");
        if (asciiFallback.isBlank()) {
            asciiFallback = "download.txt";
        }

        return "attachment; filename*=UTF-8''" + encodedFilename +
                "; filename=\"" + asciiFallback + "\"";
    }

}
