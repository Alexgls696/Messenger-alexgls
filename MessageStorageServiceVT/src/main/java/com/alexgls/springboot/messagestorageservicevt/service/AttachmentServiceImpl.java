package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageType;
import com.alexgls.springboot.messagestorageservicevt.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;

    private final ParticipantsService participantsService;

    @Override
    public List<Attachment> findAllByMediaTypeAndChatId(String mediaType, int chatId, int currentUserId) {
        boolean mediaTypeCorrect = Arrays.stream(MessageType.values()).anyMatch(value -> value.name().equals(mediaType.toUpperCase()));
        List<Integer> membersList = participantsService.findUserIdsByChatId(chatId);
        if (membersList.contains(currentUserId)) {
            MessageType messageType = MessageType.valueOf(mediaTypeCorrect ? mediaType : "FILE");
            return attachmentRepository.findAllByLogicTypeAndChatId(messageType, chatId);
        }
        throw new AccessDeniedException("Вы не состоите в данном чате.");
    }


}
