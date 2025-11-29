package com.alexgls.springboot.metadatastorageservice.dto;

import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;


public record FileMetadataDto(
        String title,
        String summary,
        List<String> topics,
        List<String>keywords,
        List<String>entities
) {
}