package com.alexgls.springboot.metadatastorageservice.service;

import com.alexgls.springboot.metadatastorageservice.dto.ElasticSearchStorageServiceRequest;
import com.alexgls.springboot.metadatastorageservice.dto.SavedMetadataNotificationMessage;
import com.alexgls.springboot.metadatastorageservice.entity.FileMetadata;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaSenderService {

    private final KafkaTemplate<String, SavedMetadataNotificationMessage> metadataNotificationMessageKafkaTemplate;

    public void sendNotification(Iterable<FileMetadata> savedMetadataIterable) {

        for (var metadata : savedMetadataIterable) {
            CompletableFuture<SendResult<String, SavedMetadataNotificationMessage>> asyncSaveResult =
                    metadataNotificationMessageKafkaTemplate.send("saved-metadata-topic",
                            new SavedMetadataNotificationMessage(metadata.chatId(), metadata.fileId(), metadata.title(), metadata.summary()));
            asyncSaveResult.whenComplete((result, throwable) -> {
                handleKafkaResultThrowable(result.getProducerRecord().value().title(), throwable);
            });
        }
    }

    private void handleKafkaResultThrowable(String titleResult, Throwable throwable) {
        if (throwable != null) {
            log.error("Error when sending via kafka: {}", throwable.getMessage());
        } else {
            log.info("Successfully sent via kafka: {}", titleResult);
        }
    }


}
