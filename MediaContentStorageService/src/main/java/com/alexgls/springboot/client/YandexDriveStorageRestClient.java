package com.alexgls.springboot.client;

import com.alexgls.springboot.client.response.DownloadFileResponse;
import com.alexgls.springboot.client.response.UploadFilePathResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class YandexDriveStorageRestClient {

    private final WebClient webClient;

    private void exceptionMessage(Exception exception) {
        log.warn("При работе с yandex client возникло исключение {}", exception.getMessage());
    }

    public Mono<UploadFilePathResponse> getUploadFilePathUrl(String path) {
        try {
            return webClient
                    .get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(UploadFilePathResponse.class);
        } catch (Exception exception) {
            exceptionMessage(exception);
            throw exception;
        }
    }


    public Mono<ResponseEntity<Void>> uploadFile(String path, FilePart filePart) {
        return filePart.content()
                .collectList()
                .flatMap(dataBuffers -> {
                    Flux<DataBuffer> body = Flux.fromIterable(dataBuffers);
                    return webClient
                            .put()
                            .uri(path)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header("Content-Length", String.valueOf(getTotalSize(dataBuffers)))
                            .body(BodyInserters.fromDataBuffers(body))
                            .retrieve()
                            .toBodilessEntity();
                });
    }

    public Mono<DownloadFileResponse> getDownloadFileUrl(String path) {
        try {
            return webClient
                    .get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(DownloadFileResponse.class);
        } catch (Exception exception) {
            exceptionMessage(exception);
            throw exception;
        }
    }

    private long getTotalSize(List<DataBuffer> dataBuffers) {
        return dataBuffers.stream()
                .mapToLong(DataBuffer::readableByteCount)
                .sum();
    }

}
