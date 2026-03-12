package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.client.S3VkCloudClient;
import com.alexgls.springboot.indatabasecontentstorageservice.dto.UploadUrlResponse;
import com.alexgls.springboot.indatabasecontentstorageservice.dto.UrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3MediaStorageService {

    private final S3VkCloudClient s3VkCloudClient;

    public UrlResponse generateDownloadUrl(String key) {
        return new UrlResponse(s3VkCloudClient.generateDownloadUrl(key));
    }

    public UploadUrlResponse generateUploadUrl(String filename, String contentType) {
        return s3VkCloudClient.generateUploadUrl(filename, contentType);
    }

}
