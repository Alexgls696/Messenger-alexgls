package com.alexgls.springboot.contentanalysisservice.dto;

public record ClickHouseStorageServiceRequest(
        FileMetadata fileMetadata,
        int chatId
) {
}
