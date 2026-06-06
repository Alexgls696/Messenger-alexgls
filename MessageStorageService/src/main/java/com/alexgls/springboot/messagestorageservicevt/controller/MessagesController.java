package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.dto.attachments.CreateAttachmentPayload;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.*;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservicevt.service.KafkaSenderService;
import com.alexgls.springboot.messagestorageservicevt.service.MessagesService;
import com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessagesController {
    private final MessagesService messagesService;

    private final KafkaSenderService kafkaSenderService;

    private final MessageMapper messageMapper;

    @PostMapping
    public ResponseEntity<MessageDto> createMessage(@RequestBody ChatMessage message, Authentication authentication) {
        var payload = messageMapper.getCreateMessagePayload(message, authentication);
        String token = SecurityUtils.getToken(authentication);
        MessageDto savedMessageDto = messagesService.save(payload, token);
        log.info("Message has been successfully saved in database: {}", savedMessageDto);
        kafkaSenderService.sendMessage(savedMessageDto);
        return ResponseEntity
                .ok(savedMessageDto);
    }

    @PostMapping("/forward")
    public ResponseEntity<List<MessageDto>> createForwardMessage(@RequestBody ForwardMessageRequest request, Authentication authentication) {
        var payload = messageMapper.getCreateMessagePayload(request.chatMessage(), authentication);
        String token = SecurityUtils.getToken(authentication);
        var savedMessages = messagesService.saveMessageWithForwardedMessages(payload, request.forwardedMessagesIds(), token);
        log.info("Forwarded messages has been successfully saved in database: {}", savedMessages);

        for (var msg : savedMessages) {
            kafkaSenderService.sendMessage(msg);
        }
        return ResponseEntity
                .ok(savedMessages);
    }

    @GetMapping
    public List<MessageDto> findMessagesByChatId(
            @RequestParam("chatId") int chatId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int pageSize,
            Authentication authentication) {
        int currentUserId = getSenderId(authentication);
        log.info("findMessagesByChatId chatId={}, page={}, size={}", chatId, page, pageSize);
        return messagesService.getMessagesByChatId(chatId, page, pageSize, currentUserId);
    }

    @GetMapping("/by-id")
    public ResponseEntity<?> findMessageById(@RequestParam Long messageId, @RequestParam Long chatId, Authentication authentication) {
        if(Objects.isNull(messageId) || Objects.isNull(chatId)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error","Чат и id сообщение должны быть указаны"));
        }
        int sender = getSenderId(authentication);
        var message =  messagesService.findById(messageId,chatId,sender);
        return ResponseEntity.ok(message);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MessageDto> updateMessage(@PathVariable("id") long messageId, @RequestBody EditMessageRequest editMessageRequest, Authentication authentication) {
        log.info("Edit message with id: {}", messageId);
        int userId = getSenderId(authentication);
        var updatedMessageDto = messagesService.updateMessage(messageId, userId, editMessageRequest);
        kafkaSenderService.sendUpdatedMessage(updatedMessageDto);
        return ResponseEntity.ok(updatedMessageDto);
    }

    @PostMapping("/find-by-content-in-chat")
    public List<MessageDto> findMessagesByContent(@RequestBody SearchMessageInChatRequest request, Authentication authentication) {
        log.info("find messages by content in the chat : {}", request);
        int userId = getSenderId(authentication);
        return messagesService.findMessagesByContent(request, userId);
    }

    @PostMapping("/read-messages")
    public void readMessagesByIdsList(@RequestBody List<ReadMessagePayload> messages, Authentication authentication) {
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
        DeleteMessageResponse response = messagesService.deleteMessages(deleteMessageRequest, currentUserId);
        kafkaSenderService.sendDeleteEventMessagesToKafka(response);
    }

}
