package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.dto.messages.*;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.service.KafkaSenderService;
import com.alexgls.springboot.messagestorageservicevt.service.MessagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessagesController {
    private final MessagesService messagesService;

    private final KafkaSenderService kafkaSenderService;

    @GetMapping
    public List<Message> findMessagesByChatId(
            @RequestParam("chatId") int chatId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int pageSize,
            Authentication authentication) {
        int currentUserId = getSenderId(authentication);
        log.info("findMessagesByChatId chatId={}, page={}, size={}", chatId, page, pageSize);
        return messagesService.getMessagesByChatId(chatId, page, pageSize, currentUserId)
                .stream()
                .toList();
    }

    @PatchMapping("/{id}")
    public MessageDto updateMessage(@PathVariable("id") long messageId, @RequestBody EditMessageRequest editMessageRequest, Authentication authentication) {
        log.info("Edit message with id: {}", messageId);
        int userId = getSenderId(authentication);
        var updatedMessageDto = messagesService.updateMessage(messageId, userId, editMessageRequest);
        kafkaSenderService.sendUpdatedMessage(updatedMessageDto);
        return updatedMessageDto;
    }

    @PostMapping("/find-by-content-in-chat")
    public List<MessageDto> findMessagesByChatId(@RequestBody SearchMessageInChatRequest request, Authentication authentication) {
        log.info("find messages by content in the chat : {}", request);
        int userId = getSenderId(authentication);
        return messagesService.findMessagesByContent(request, userId);
    }

    @PostMapping("/read-messages")
    public void readMessagesList(@RequestBody List<ReadMessagePayload> messages, Authentication authentication) {
        int currentUserId = getSenderId(authentication);
        final List<ReadMessagePayload> filteredMessages = messages
                .stream()
                .filter(message -> message.senderId() != currentUserId)
                .toList();
        log.info("Read messages from payload... {}", filteredMessages);
        messagesService.readMessagesByList(filteredMessages, currentUserId);
        kafkaSenderService.sendReadMessagesToKafka(filteredMessages);
    }

    @DeleteMapping
    public void deleteMessage(@RequestBody DeleteMessageRequest deleteMessageRequest, Authentication authentication) {
        int currentUserId = getSenderId(authentication);
        log.info("Try to delete messages: {} ", deleteMessageRequest);
        DeleteMessageResponse response = messagesService.deleteById(deleteMessageRequest, currentUserId);
        kafkaSenderService.sendDeleteEventMessagesToKafka(response);
    }

}
