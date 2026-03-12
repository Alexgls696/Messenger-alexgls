package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.indatabasecontentstorageservice.entity.FileMetadata;
import com.alexgls.springboot.indatabasecontentstorageservice.exception.NoSuchImageException;
import com.alexgls.springboot.indatabasecontentstorageservice.repository.FilesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FilesServiceImpl implements FilesService {

    private final FilesRepository filesRepository;

    @Override
    public FileMetadata findById(int id) {
        return filesRepository.findById(id)
                .orElseThrow(() -> new NoSuchImageException("Image with id %d not found".formatted(id)));
    }

    @Override
    public void deleteById(int id) {
        filesRepository.deleteById(id);
    }

    @Override
    public FileMetadata save(CreateFileMetadataRequest createFileMetadataRequest) {
        FileMetadata chatImage = new FileMetadata();
        chatImage.setPath(createFileMetadataRequest.path());
        chatImage.setFilename(createFileMetadataRequest.filename());
        return filesRepository.save(chatImage);
    }
}
