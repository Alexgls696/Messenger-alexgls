package com.alexgls.springboot.metadatastorageservice.mapper;

import com.alexgls.springboot.metadatastorageservice.dto.ElasticSearchStorageServiceRequest;
import com.alexgls.springboot.metadatastorageservice.dto.FileMetadataResponse;
import com.alexgls.springboot.metadatastorageservice.entity.FileMetadata;

import java.util.UUID;

public class FileMetadataMapper {
    public static FileMetadata requestToEntity(ElasticSearchStorageServiceRequest request) {
        return new FileMetadata(UUID.randomUUID().toString(),
                request.fileId(),
                request.chatId(),
                request.fileMetadataDto().title(),
                request.fileMetadataDto().summary(),
                request.fileMetadataDto().topics(),
                request.fileMetadataDto().keywords(),
                request.fileMetadataDto().entities());
    }

    public static FileMetadataResponse entityToResponse(FileMetadata fileMetadata) {
        return new FileMetadataResponse(fileMetadata.fileId(),
                fileMetadata.chatId(),
                fileMetadata.title(),
                fileMetadata.summary(),
                fileMetadata.topics(),
                fileMetadata.keywords(),
                fileMetadata.entities());
    }
}
