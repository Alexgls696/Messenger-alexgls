package com.alexgls.springboot.contentanalysisservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyseFileRequest{
    private String key;
    private int chatId;
    private int fileId;
    private String fileName;
    int dlqRetryCount = 0;
}