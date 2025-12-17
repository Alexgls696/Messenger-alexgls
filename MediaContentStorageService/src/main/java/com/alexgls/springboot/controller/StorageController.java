package com.alexgls.springboot.controller;

import com.alexgls.springboot.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> saveFile(@RequestPart("file") FilePart file,
                                            @RequestPart(value = "isAnalyse", required = false) String isAnalyseStr,
                                            @RequestPart(value = "chatId", required = false) String chatIdStr,
                                            Authentication authentication) {
        log.info("Uploading file...");

        boolean isAnalyse = Boolean.parseBoolean(isAnalyseStr);

        Integer chatId = null;
        if (chatIdStr != null && !chatIdStr.isEmpty()) {
            try {
                chatId = Integer.parseInt(chatIdStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid chatId format received: {}", chatIdStr);
                // Возвращаем ошибку, если chatId пришел, но он не является числом
                return Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid chatId format", "code", HttpStatus.BAD_REQUEST.value())));
            }
        }

        if (Objects.isNull(file)) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File is null", "code", HttpStatus.BAD_REQUEST.value())));
        }

        String token = getCurrentUserId(authentication);
        return storageService.uploadFile(file, isAnalyse, chatId, token)
                .map(ResponseEntity::ok);
    }

    @Deprecated
    @GetMapping("/download/by-path")
    public Mono<ResponseEntity<Map<String, String>>> getDownloadLinkByPath(@RequestParam("path") String path) {
        log.info("Get downloadLink by path: {}", path);
        return storageService.getDownLoadFilePath(path)
                .map(resultPath -> ResponseEntity
                        .ok()
                        .body(Map.of("href", resultPath)));
    }

    @Deprecated
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

    @DeleteMapping("/delete/by-id")
    public Mono<ResponseEntity<Void>> deleteById(@RequestParam("id") Integer id) {
        if (Objects.isNull(id)) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
        log.info("Deleting image from storage: {}", id);
        return storageService.removeFileById(id)
                .then(Mono.just(ResponseEntity
                        .status(HttpStatus.NO_CONTENT)
                        .build()));
    }

    private String getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getTokenValue();
    }


}
