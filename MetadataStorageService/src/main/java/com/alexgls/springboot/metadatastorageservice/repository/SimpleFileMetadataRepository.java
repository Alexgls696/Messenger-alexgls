package com.alexgls.springboot.metadatastorageservice.repository;

import com.alexgls.springboot.metadatastorageservice.entity.FileMetadata;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimpleFileMetadataRepository  extends ElasticsearchRepository<FileMetadata, String> {

    List<FileMetadata> findAllByChatId(int chatId);
}
