package com.alexgls.springboot.metadatastorageservice.repository;

import com.alexgls.springboot.metadatastorageservice.dto.ElasticSearchStorageServiceRequest;
import com.alexgls.springboot.metadatastorageservice.entity.FileMetadata;

import java.util.List;

public interface MetadataRepository {
    void saveAllMetadata(List<ElasticSearchStorageServiceRequest> records);
    List<FileMetadata> searchInChat(int chatId, String queryText);
}
