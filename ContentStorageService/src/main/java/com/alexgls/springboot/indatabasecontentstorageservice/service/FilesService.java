package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.indatabasecontentstorageservice.entity.FileMetadata;

public interface FilesService {
    FileMetadata findById(int id, int userId, String token);

    void deleteById(int id);

    FileMetadata save(CreateFileMetadataRequest createFileMetadataRequest);
}
