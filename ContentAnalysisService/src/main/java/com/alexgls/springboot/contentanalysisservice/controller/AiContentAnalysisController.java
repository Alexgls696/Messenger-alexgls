package com.alexgls.springboot.contentanalysisservice.controller;

import com.alexgls.springboot.contentanalysisservice.dto.AnalyseFileRequest;
import com.alexgls.springboot.contentanalysisservice.service.AiContentAnalysisService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
@Slf4j
public class AiContentAnalysisController {

    private final AiContentAnalysisService aiContentAnalysisService;

    @PostMapping
    public ResponseEntity<Void> loadAndAnalyseFileRequest(@RequestBody AnalyseFileRequest analyseFileRequest) {

        aiContentAnalysisService.analyseFile(analyseFileRequest)
                .exceptionally(ex -> {
                    log.error("Ошибка при анализе файла id: {}", analyseFileRequest.getFileId(), ex);
                    return null;
                });

        return ResponseEntity.accepted().build();
    }
}


