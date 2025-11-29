package com.alexgls.springboot.metadatastorageservice.service;

import com.alexgls.springboot.metadatastorageservice.dto.ElasticSearchStorageServiceRequest;
import com.alexgls.springboot.metadatastorageservice.dto.FileMetadataResponse;


import java.util.List;

public interface MetadataService {
    void saveAllRecords(List<ElasticSearchStorageServiceRequest> records);
     List<FileMetadataResponse> findAllByChatIdAndQuery(int chatId, String queryText);

     List<FileMetadataResponse>findAllByChatId(int chatId);

    FileMetadataResponse findByFileId(int fileId);
}
