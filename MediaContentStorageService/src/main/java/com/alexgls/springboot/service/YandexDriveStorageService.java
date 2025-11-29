package com.alexgls.springboot.service;

import com.alexgls.springboot.client.ContentAnalysisClient;
import com.alexgls.springboot.client.InDatabaseStorageServiceRestClient;
import com.alexgls.springboot.client.YandexDriveStorageRestClient;
import com.alexgls.springboot.client.response.DownloadFileResponse;
import com.alexgls.springboot.client.response.UploadFilePathResponse;
import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.dto.CreateFileResponse;
import com.alexgls.springboot.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    private final ContentAnalysisClient contentAnalysisClient;

    @Value("${yandex.baseUrl}")
    private String baseUrl;

    @Value("${yandex.application-path}")
    private String applicationPath;

    @Override
    public Mono<CreateFileResponse> uploadFile(FilePart file, boolean isAnalyse, int chatId, String token) {
        String filepathToDatabase = getFilePath(file.filename());
        String uploadUrl = baseUrl + "/upload?path=" + filepathToDatabase;

        Mono<UploadFilePathResponse> uploadPathMono =
                yandexDriveRestClient.getUploadFilePathUrl(uploadUrl);

        Mono<ChatImage> chatImageMono =
                inDatabaseStorageServiceRestClient.saveChatImage(
                        new CreateFileMetadataRequest(filepathToDatabase, file.filename())
                );

        return uploadPathMono
                .flatMap(uploadPath ->
                        saveFileToYandexDrive(file, uploadPath)
                                .then(chatImageMono)
                                .map(chatImage ->
                                        new CreateFileResponse(
                                                chatImage.getId(),
                                                filepathToDatabase,
                                                Timestamp.from(Instant.now())
                                        )
                                )
                )
                .doOnSuccess(response -> {
                    if (isAnalyse) {
                        contentAnalysisClient.sendFileForAnalysis(file, response.id(), chatId, token)
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnError(err -> log.warn("Analysis failed: {}", err.getMessage()))
                                .subscribe();
                    }
                })
                .switchIfEmpty(Mono.error(
                        new FileUploadException("Не удалось загрузить файл: " + file.filename())
                ));
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

    @Override
    public Mono<ChatImage> getFileMetadataById(int id) {
        Mono<ChatImage> chatImageMono = inDatabaseStorageServiceRestClient.findChatImageById(id);
        return chatImageMono
                .flatMap(chatImage ->
                        findFileHrefInYandex(chatImage.getPath())
                                .map(href -> {
                                    chatImage.setPath(href);
                                    return chatImage;
                                })
                );
    }

    @Override
    public Mono<Void> removeFileById(int id) {
        return inDatabaseStorageServiceRestClient.findChatImageById(id)
                .flatMap(chatImage -> Mono.empty());
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
