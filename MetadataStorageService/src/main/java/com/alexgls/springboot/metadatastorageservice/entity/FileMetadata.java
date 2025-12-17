package com.alexgls.springboot.metadatastorageservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Document(indexName = "file_metadata")
public record FileMetadata(
        @Id
        String id,

        @Field(type = FieldType.Long)
        int fileId,

        @Field(type = FieldType.Integer)
        int chatId,

        @Field(type = FieldType.Text, analyzer = "russian")
        String title,

        @Field(type = FieldType.Text, analyzer = "russian")
        String summary,

        @Field(type = FieldType.Keyword)
        List<String> topics,

        @Field(type = FieldType.Keyword)
        List<String> keywords,

        @Field(type = FieldType.Keyword)
        List<String> entities
) {
}
