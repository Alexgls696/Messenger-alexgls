package com.alexgls.springboot.controller;

import com.alexgls.springboot.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

//    http://localhost:8080/api/storage/upload

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> saveFile(@RequestPart("file") FilePart file) {
        log.info("Uploading image to storage");
        if (Objects.isNull(file)) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File is null", "code", HttpStatus.BAD_REQUEST.value())));
        }
        return storageService.uploadImage(file)
                .map(createFileResponse -> {
                    log.info("Uploading image to storage");
                    return ResponseEntity.ok(createFileResponse);
                });
    }

    @GetMapping("/download/by-path")
    public Mono<ResponseEntity<Map<String, String>>> getDownloadLinkByPath(@RequestParam("path") String path) {
        log.info("Get downloadLink by path: {}", path);
        return storageService.getDownLoadFilePath(path)
                .map(resultPath -> ResponseEntity
                        .ok()
                        .body(Map.of("href", resultPath)));
    }

    @GetMapping("/download/by-id")
    public Mono<ResponseEntity<Map<String, String>>> getDownloadLinkById(@RequestParam("id") Integer id) {
        if (Objects.isNull(id)) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
        log.info("Downloading image from storage: {}", id);
        return storageService.getDownloadPathById(id)
                .map(resultPath -> ResponseEntity
                        .ok()
                        .body(Map.of("href", resultPath)));
    }

}
