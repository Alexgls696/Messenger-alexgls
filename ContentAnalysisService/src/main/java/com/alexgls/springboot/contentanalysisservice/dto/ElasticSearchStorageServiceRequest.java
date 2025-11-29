package com.alexgls.springboot.contentanalysisservice.dto;

public record ElasticSearchStorageServiceRequest(
        FileMetadataDto fileMetadataDto,
        int chatId,
        int fileId
) {
}
