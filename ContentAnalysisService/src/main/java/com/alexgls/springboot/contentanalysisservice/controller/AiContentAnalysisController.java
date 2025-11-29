package com.alexgls.springboot.contentanalysisservice.controller;

import com.alexgls.springboot.contentanalysisservice.service.AiContentAnalysisService;
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
    public CompletableFuture<ResponseEntity<Void>> loadAndAnalyseFileRequest(@RequestParam("file") MultipartFile file, @RequestParam("chatId") int chatId, @RequestParam("fileId") int fileId) {
        log.info("LoadAndAnalyseFileRequest, file: {}", file.getOriginalFilename());

        Resource resource;
        try {
            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename();

            resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename != null ? filename : "unknown";
                }
            };
        } catch (IOException e) {
            log.error("Ошибка чтения файла", e);
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .build());
        }

        aiContentAnalysisService.analyseFile(resource, chatId, fileId)
                .exceptionally(ex -> {
                    log.error("Ошибка при асинхронном анализе файла", ex);
                    return null;
                });

        return CompletableFuture.completedFuture(ResponseEntity
                .accepted()
                .build());
    }
}


