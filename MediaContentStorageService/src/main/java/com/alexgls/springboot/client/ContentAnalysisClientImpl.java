package com.alexgls.springboot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
public class ContentAnalysisClientImpl implements ContentAnalysisClient {

    private final WebClient webClient;

    @Override
    public Mono<Void> sendFileForAnalysis(FilePart file, int fileId, int chatId, String token) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    MultipartBodyBuilder builder = new MultipartBodyBuilder();

                    builder.part("fileId", fileId);
                    builder.part("chatId", chatId);

                    builder.part("file", bytes)
                            .filename(file.filename())
                            .contentType(MediaType.APPLICATION_OCTET_STREAM);

                    return webClient.post()
                            .uri("/api/analysis")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header("Authorization", "Bearer " + token)
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(Void.class);
                });
    }


}
