package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;
import com.alexgls.springboot.messagestorageservicevt.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.getSenderId;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentsController {
    private final AttachmentService attachmentService;

    @GetMapping("/find-by-type-and-chat-id")
    public List<Attachment> getAttachmentsByChatIdAndMimeType(
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) Integer chatId,
            Authentication auth) {

        log.info("getAttachmentsByChatIdAndMimeType: mediaType={}, chatId={}", mediaType, chatId);

        if (mediaType == null || chatId == null) {
            throw  new IllegalArgumentException("Обязательные параметры: mimeType и chatId");
        }

        int currentUserId = getSenderId(auth);

        return attachmentService.findAllByMediaTypeAndChatId(mediaType, chatId, currentUserId);
    }

}
