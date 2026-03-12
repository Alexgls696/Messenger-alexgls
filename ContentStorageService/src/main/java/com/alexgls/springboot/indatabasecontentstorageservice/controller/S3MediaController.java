package com.alexgls.springboot.indatabasecontentstorageservice.controller;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.*;
import com.alexgls.springboot.indatabasecontentstorageservice.service.S3MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/media-storage")
@RequiredArgsConstructor
public class S3MediaController {

    private final S3MediaStorageService s3MediaStorageService;

    @PostMapping("/generate-download-url")
    public UrlResponse generateDownloadUrl(@RequestBody DownloadUrlRequest request) {
        return s3MediaStorageService.generateDownloadUrl(request.url());
    }

    @PostMapping("/upload-url")
    public UploadUrlResponse generateUploadUrl(@RequestBody UploadUrlRequest request) {
        return s3MediaStorageService.generateUploadUrl(request.fileName(), request.contentType());
    }

}