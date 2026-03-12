package com.alexgls.springboot.indatabasecontentstorageservice.controller;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.indatabasecontentstorageservice.entity.FileMetadata;
import com.alexgls.springboot.indatabasecontentstorageservice.service.FilesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FilesController {

    private final FilesService filesService;

    @GetMapping("/{id}")
    public FileMetadata findChatImageById(@PathVariable("id") int id) {
        return filesService.findById(id);
    }

    @PostMapping
    public ResponseEntity<FileMetadata> saveFile(@RequestBody CreateFileMetadataRequest createFileMetadataRequest, UriComponentsBuilder uriBuilder) {
        log.info("Save file: {}", createFileMetadataRequest);
        FileMetadata created = filesService.save(createFileMetadataRequest);
        return ResponseEntity
                .created(uriBuilder.replacePath("/api/files/{id}")
                        .build(Map.of("id", created.getId())))
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatImage(@PathVariable("id") int id) {
        log.info("Delete chat image by id: {}", id);
        filesService.deleteById(id);
        return ResponseEntity
                .noContent()
                .build();
    }

}
