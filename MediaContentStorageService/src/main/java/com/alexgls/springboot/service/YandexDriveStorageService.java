package com.alexgls.springboot.service;

import com.alexgls.springboot.client.InDatabaseStorageServiceRestClient;
import com.alexgls.springboot.client.YandexDriveStorageRestClient;
import com.alexgls.springboot.client.response.DownloadFileResponse;
import com.alexgls.springboot.client.response.UploadFilePathResponse;
import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.dto.CreateFileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class YandexDriveStorageService implements StorageService {

    private final YandexDriveStorageRestClient yandexDriveRestClient;

    private final InDatabaseStorageServiceRestClient inDatabaseStorageServiceRestClient;

    @Value("${yandex.baseUrl}")
    private String baseUrl;

    @Value("${yandex.application-path}")
    private String applicationPath;

    @Override
    public Mono<CreateFileResponse> uploadImage(FilePart file) {
        StringBuilder pathBuilder = new StringBuilder();
        String filepathToDatabase = getFilePath(file.filename());
        pathBuilder.append(baseUrl).append("/upload?path=").append(filepathToDatabase);
        Mono<UploadFilePathResponse> uploadPathResponseMono = yandexDriveRestClient.getUploadFilePathUrl(pathBuilder.toString());
        Mono<ChatImage> chatImageMono = inDatabaseStorageServiceRestClient.saveChatImage(new CreateFileMetadataRequest(filepathToDatabase, file.filename()));

        return Mono.zip(uploadPathResponseMono, chatImageMono)
                .flatMap(tuple -> {
                    UploadFilePathResponse uploadFilePathResponse = tuple.getT1();
                    ChatImage chatImageFromDatabase = tuple.getT2();
                    log.info("upload path from yandex {}", uploadFilePathResponse);
                    log.info("Saving path to database... {}", filepathToDatabase);
                    return saveFileToYandexDrive(file, uploadFilePathResponse)
                            .then(Mono.fromCallable(() -> new CreateFileResponse(chatImageFromDatabase.getId(), filepathToDatabase, Timestamp.from(Instant.now()))));
                });
    }

    @Override
    public Mono<String> getDownLoadFilePath(String path) {
        return findFileHrefInYandex(path);
    }

    @Override
    public Mono<String> getDownloadPathById(int id) {
        return inDatabaseStorageServiceRestClient.findChatImageById(id)
                .flatMap(chatImage -> findFileHrefInYandex(chatImage.getPath()));
    }

    private Mono<String> findFileHrefInYandex(String path) {
        path = baseUrl + "/download?path=" + path;
        return yandexDriveRestClient.getDownloadFileUrl(path)
                .map(result -> {
                    log.info("download file url from yandex {}", result);
                    return result.href();
                });
    }

    private String getFilePath(String fileName) {
        int lastPointIndex = fileName.lastIndexOf(".");
        String format = fileName.substring(lastPointIndex);
        String randomFileName = UUID.randomUUID() + format;
        return applicationPath + "/images/" + randomFileName;
    }

    private Mono<Void> saveFileToYandexDrive(FilePart file, UploadFilePathResponse response) {
        return yandexDriveRestClient.uploadFile(response.getHref(), file)
                .doOnSuccess(unused -> log.info("Upload file success to href: {}", response.getHref()))
                .doOnError(error -> log.warn("Upload file failed, href: {}, error: {}",
                        response.getHref(), error.getMessage())).then();
    }


}
