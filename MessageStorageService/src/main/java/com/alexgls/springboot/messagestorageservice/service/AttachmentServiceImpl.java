package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import com.alexgls.springboot.messagestorageservice.entity.MessageType;
import com.alexgls.springboot.messagestorageservice.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;

    private final ParticipantsService participantsService;

    @Override
    public Flux<Attachment> findAllByMediaTypeAndChatId(String mediaType, int chatId, int currentUserId) {
        boolean mediaTypeCorrect = Arrays.stream(MessageType.values()).anyMatch(value -> value.name().equals(mediaType.toUpperCase()));
        return participantsService.findUserIdsByChatId(chatId)
                .collectList()
                .flatMapMany(membersList -> {
                    if (membersList.contains(currentUserId)) {
                        try {
                            MessageType messageType = MessageType.valueOf(mediaTypeCorrect ? mediaType : "FILE");
                            return attachmentRepository.findAllByLogicTypeAndChatId(messageType, chatId);
                        } catch (IllegalArgumentException exception) {
                            return Flux.error(exception);
                        }
                    }
                    return Flux.error(() -> new AccessDeniedException("Вы не состоите в данном чате."));
                });
    }
}
