package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.client.MessageStorageRestClient;
import com.alexgls.springboot.indatabasecontentstorageservice.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.indatabasecontentstorageservice.entity.FileMetadata;
import com.alexgls.springboot.indatabasecontentstorageservice.exception.NoSuchImageException;
import com.alexgls.springboot.indatabasecontentstorageservice.mapper.FileMetadataMapper;
import com.alexgls.springboot.indatabasecontentstorageservice.repository.FilesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class FilesServiceImpl implements FilesService {

    private final FilesRepository filesRepository;

    private final MessageStorageRestClient messageStorageRestClient;

    @Override
    public FileMetadata findById(int id, int userId, String token) {
        FileMetadata fileMetadata = filesRepository.findById(id)
                .orElseThrow(() -> new NoSuchImageException("Image with id %d not found".formatted(id)));
        if(fileMetadata.getSecurityType().equals("private")){
            var exists = messageStorageRestClient.existsByChatIdAndUserId(fileMetadata.getChatId(), userId, token).exists();
            if(exists){
                return fileMetadata;
            }else{
                throw new AccessDeniedException("У вас нет прав на выполнение этой операции");
            }
        }
        return fileMetadata;
    }

    @Override
    public void deleteById(int id) {
        filesRepository.deleteById(id);
    }

    @Override
    public FileMetadata save(CreateFileMetadataRequest createFileMetadataRequest) {
        FileMetadata chatImage = FileMetadataMapper.toFileMetadata(createFileMetadataRequest);
        return filesRepository.save(chatImage);
    }
}
