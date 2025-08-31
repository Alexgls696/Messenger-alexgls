package com.alexgls.springboot.controller;

import com.alexgls.springboot.service.FileProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/storage/proxy")
@Slf4j
public class ProxyController {

    private final FileProxyService fileProxyService;

    @PreAuthorize("permitAll()")
    @GetMapping("/download/by-id")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFileById(@RequestParam("id") Integer id) {
        log.info("Try to download file by id: {}", id);
        return fileProxyService.downloadAndStreamFileById(id);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/download/by-path")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFileByPath(@RequestParam("path") String path) {
        log.info("Try to download file by path: {}", path);
        return fileProxyService.downloadAndStreamFileByPath(path);
    }
}
