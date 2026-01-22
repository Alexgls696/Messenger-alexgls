package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import com.alexgls.springboot.messagestorageservice.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import static com.alexgls.springboot.messagestorageservice.util.SecurityUtils.*;


@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentsController {

    private final AttachmentService attachmentService;

    @GetMapping("/find-by-type-and-chat-id")
    public Flux<Attachment> getAttachmentsByChatIdAndMimeType(
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) Integer chatId,
            Authentication auth) {

        log.info("getAttachmentsByChatIdAndMimeType: mediaType={}, chatId={}", mediaType, chatId);

        if (mediaType == null || chatId == null) {
            return Flux.error(() -> new IllegalArgumentException("Обязательные параметры: mimeType и chatId"));
        }

        int currentUserId = getSenderId(auth);
        return attachmentService.findAllByMediaTypeAndChatId(mediaType, chatId, currentUserId);
    }

}
