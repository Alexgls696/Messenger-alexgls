package com.alexgls.springboot.metadatastorageservice.dto;

public record ElasticSearchStorageServiceRequest(
        FileMetadataDto fileMetadataDto,
        int chatId,
        int fileId
) {
}
