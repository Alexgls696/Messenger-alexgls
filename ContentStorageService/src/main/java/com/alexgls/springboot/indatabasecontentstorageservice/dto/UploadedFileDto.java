package com.alexgls.springboot.indatabasecontentstorageservice.dto;

public record UploadedFileDto(
        String key,
        String url
) {}