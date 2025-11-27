package com.alexgls.springboot.contentanalysisservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoadFileResponse(
        String id,
        String filename
) {
}
