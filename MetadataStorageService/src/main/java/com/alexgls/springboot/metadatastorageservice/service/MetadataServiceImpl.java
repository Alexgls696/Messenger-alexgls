package com.alexgls.springboot.metadatastorageservice.service;

import com.alexgls.springboot.metadatastorageservice.dto.ElasticSearchStorageServiceRequest;
import com.alexgls.springboot.metadatastorageservice.dto.FileMetadataResponse;
import com.alexgls.springboot.metadatastorageservice.entity.FileMetadata;
import com.alexgls.springboot.metadatastorageservice.exception.NoSuchMetadataException;
import com.alexgls.springboot.metadatastorageservice.mapper.FileMetadataMapper;
import com.alexgls.springboot.metadatastorageservice.repository.MetadataRepository;
import com.alexgls.springboot.metadatastorageservice.repository.SimpleFileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetadataServiceImpl implements MetadataService {

    private final MetadataRepository metadataRepository;

    private final SimpleFileMetadataRepository simpleFileMetadataRepository;

    @Override
    public void saveAllRecords(List<ElasticSearchStorageServiceRequest> records) {
        metadataRepository.saveAllMetadata(records);
    }

    @Override
    public List<FileMetadataResponse> findAllByChatIdAndQuery(int chatId, String queryText) {
        return metadataRepository.searchInChat(chatId, queryText)
                .stream().map(FileMetadataMapper::entityToResponse)
                .toList();
    }

    @Override
    public List<FileMetadataResponse> findAllByChatId(int chatId) {
        return simpleFileMetadataRepository.findAllByChatId(chatId)
                .stream()
                .map(FileMetadataMapper::entityToResponse)
                .toList();
    }

    @Override
    public FileMetadataResponse findByFileId(int fileId) {
        var metadata = simpleFileMetadataRepository
                .findByFileId(fileId)
                .orElseThrow(()->new NoSuchMetadataException("Метаданные файла с заданным id не найдены."));
        return FileMetadataMapper.entityToResponse(metadata);
    }
}
