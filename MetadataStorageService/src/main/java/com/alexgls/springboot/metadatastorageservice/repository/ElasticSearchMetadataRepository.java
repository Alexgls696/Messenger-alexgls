package com.alexgls.springboot.metadatastorageservice.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.alexgls.springboot.metadatastorageservice.dto.ElasticSearchStorageServiceRequest;
import com.alexgls.springboot.metadatastorageservice.entity.FileMetadata;
import com.alexgls.springboot.metadatastorageservice.mapper.FileMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElasticSearchMetadataRepository implements MetadataRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void saveAllMetadata(List<ElasticSearchStorageServiceRequest> records) {
        List<FileMetadata> toSaveMetadataList = records.stream()
                .map(FileMetadataMapper::requestToEntity)
                .toList();
        elasticsearchOperations.save(toSaveMetadataList);
    }

    @Override
    public List<FileMetadata> searchInChat(int chatId, String queryText) {
        var queryBuilder = NativeQuery.builder()
                // 1. Жесткий фильтр по chatId
                .withFilter(f -> f.term(t -> t.field("chatId").value(chatId)));

        if (queryText != null && !queryText.isBlank()) {
            // 2. Multi-match поиск по полям
            queryBuilder.withQuery(q -> q
                    .multiMatch(m -> m
                            .fields("title^3", "summary", "topics", "keywords", "entities") //приоритет в названиях
                            .query(queryText)
                            .operator(Operator.Or)
                            .fuzziness("1") // опечатки
                    )
            );
        }

        return elasticsearchOperations.search(queryBuilder.build(), FileMetadata.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}