package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.indatabasecontentstorageservice.entity.ChatImage;
import com.alexgls.springboot.indatabasecontentstorageservice.exception.NoSuchImageException;
import com.alexgls.springboot.indatabasecontentstorageservice.repository.ImagesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FilesServiceImpl implements FilesService {

    private final ImagesRepository imagesRepository;

    @Override
    public ChatImage findById(int id) {
        return imagesRepository.findById(id)
                .orElseThrow(() -> new NoSuchImageException("Image with id %d not found".formatted(id)));
    }

    @Override
    public void deleteById(int id) {
        imagesRepository.deleteById(id);
    }

    @Override
    public ChatImage save(CreateFileMetadataRequest createFileMetadataRequest) {
        ChatImage chatImage = new ChatImage();
        chatImage.setPath(createFileMetadataRequest.path());
        chatImage.setFilename(createFileMetadataRequest.filename());
        return imagesRepository.save(chatImage);
    }
}
