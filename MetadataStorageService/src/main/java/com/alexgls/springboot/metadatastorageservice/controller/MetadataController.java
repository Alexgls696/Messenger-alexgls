package com.alexgls.springboot.metadatastorageservice.controller;

import com.alexgls.springboot.metadatastorageservice.dto.ElasticSearchStorageServiceRequest;
import com.alexgls.springboot.metadatastorageservice.dto.FileMetadataResponse;
import com.alexgls.springboot.metadatastorageservice.dto.FindInChatRequest;
import com.alexgls.springboot.metadatastorageservice.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
@Slf4j
public class MetadataController {

    private final MetadataService metadataService;

    @KafkaListener(topics = "metadata-topic", groupId = "metadata-group", containerFactory = "elasticSearchListenerContainerFactory")
    public void listenMetadataTopic(List<ConsumerRecord<String, ElasticSearchStorageServiceRequest>> records, Acknowledgment ack) {
        if (records.isEmpty()) return;
        log.info("Получены метаданные в количестве: {}", records.size());

        List<ElasticSearchStorageServiceRequest> metadataList = records
                .stream()
                .map(ConsumerRecord::value)
                .toList();

        try {
            metadataService.saveAllRecords(metadataList);
            ack.acknowledge();
        } catch (Exception exception) {
            log.error("Ошибка сохранения в Elasticsearch", exception);
        }
    }

    @PostMapping
    public List<FileMetadataResponse> findAllInChat(@RequestBody FindInChatRequest findInChatRequest) {
        log.info("Find in chat request: {}", findInChatRequest);
        return metadataService.findByFileId(findInChatRequest.chatId(), findInChatRequest.query());
    }

    @GetMapping("/find-all-by-chat-id/{id}")
    public List<FileMetadataResponse> findAllByChatId(@PathVariable("id") int id) {
        log.info("Find all by chat id: {}", id);
        return metadataService.findAllByChatId(id);
    }
}
