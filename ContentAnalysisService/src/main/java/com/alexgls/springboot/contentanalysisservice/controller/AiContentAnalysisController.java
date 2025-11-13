package com.alexgls.springboot.contentanalysisservice.controller;

import com.alexgls.springboot.contentanalysisservice.dto.FileMetadata;
import com.alexgls.springboot.contentanalysisservice.service.AiContentAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
@Slf4j
public class AiContentAnalysisController {

    private final AiContentAnalysisService aiContentAnalysisService;

    @PostMapping("/{fileId}")
    public CompletableFuture<ResponseEntity<FileMetadata>> authorizationRequest(@PathVariable("fileId") String fileId) {
        log.info("Authorization Request");
        CompletableFuture<FileMetadata> resultFuture = aiContentAnalysisService.getAiContentAnalysisFromFile(fileId);
        return resultFuture
                .thenApply(result -> ResponseEntity
                        .ok()
                        .body(result));
    }

}
