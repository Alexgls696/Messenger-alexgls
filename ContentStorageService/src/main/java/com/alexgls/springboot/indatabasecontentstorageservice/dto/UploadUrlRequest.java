package com.alexgls.springboot.indatabasecontentstorageservice.dto;

public record UploadUrlRequest(
        String fileName,
        String contentType
) {}