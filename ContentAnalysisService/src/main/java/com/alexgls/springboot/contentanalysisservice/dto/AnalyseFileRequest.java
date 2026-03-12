package com.alexgls.springboot.contentanalysisservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseFileRequest{
    private String key;
    private int chatId;
    private int fileId;
    private String fileName;
}