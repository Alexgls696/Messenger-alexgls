package com.alexgls.springboot.client;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface ContentAnalysisClient {
    Mono<Void> sendFileForAnalysis(FilePart file, int fileId, int chatId, String token);
}
