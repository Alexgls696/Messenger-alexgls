package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.indatabasecontentstorageservice.entity.ChatImage;

public interface FilesService {
    ChatImage findById(int id);

    void deleteById(int id);

    ChatImage save(CreateFileMetadataRequest createFileMetadataRequest);
}
