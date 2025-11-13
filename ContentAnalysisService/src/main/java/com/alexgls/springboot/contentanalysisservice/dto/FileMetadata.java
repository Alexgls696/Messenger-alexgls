package com.alexgls.springboot.contentanalysisservice.dto;

import java.util.List;

public record FileMetadata(
        String title,
        String summary,
        List<String> topics,
        List<String>keywords,
        List<String>entities
) {
}
