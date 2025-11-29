package com.alexgls.springboot.metadatastorageservice.dto;

import com.alexgls.springboot.metadatastorageservice.entity.FileMetadata;

public record ElasticSearchStorageServiceRequest(
        FileMetadataDto fileMetadataDto,
        int chatId,
        int fileId
) {
}
