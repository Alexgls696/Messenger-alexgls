package com.alexgls.springboot.indatabasecontentstorageservice.dto;

public record CreateFileMetadataRequest(
        String path,
        String filename
) {
}
