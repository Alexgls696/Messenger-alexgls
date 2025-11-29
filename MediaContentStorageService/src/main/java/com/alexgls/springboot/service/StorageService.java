package com.alexgls.springboot.service;

import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.dto.CreateFileResponse;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface StorageService {
    Mono<CreateFileResponse> uploadFile(FilePart file, boolean isAnalyse, int chatId, String token);

    Mono<String> getDownLoadFilePath(String path);

    Mono<String> getDownloadPathById(int id);

    Mono<ChatImage> getFileMetadataById(int id);

    Mono<Void> removeFileById(int id);
}

