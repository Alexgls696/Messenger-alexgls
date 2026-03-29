package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.dto.messages.DeleteMessageRequest;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.DeleteMessageResponse;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservicevt.repository.*;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import com.alexgls.springboot.messagestorageservicevt.service.nlp.LexicalAnalyzer;
import com.alexgls.springboot.messagestorageservicevt.service.transactional.MessagesServiceTransactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagesServiceTest {

    @InjectMocks
    MessagesService messagesService;

    @Mock
    MessagesRepository messagesRepository;
    @Mock
    ChatsRepository chatsRepository;
    @Mock
    AttachmentRepository attachmentRepository;
    @Mock
    ParticipantsRepository participantsRepository;
    @Mock
    MessageTokenRepository messageTokenRepository;
    @Mock
    LexicalAnalyzer lexicalAnalyzer;
    @Mock
    MessagesServiceTransactional messagesServiceTransactional;
    @Mock
    LexicalAnalyserService lexicalAnalyserService;
    @Mock
    MessageMapper messageMapper;


    @Test
    void deleteMessages_ShouldCallDeleteMessagesForAll_WhenForAllIsTrue() {
        //given
        var messagesIds = List.of(2L, 5L); int senderId = 3;
        int currentUserId = 3; int chatId = 17; boolean forAll = true;
        DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(messagesIds, senderId, chatId, forAll);
        Message first = Message.builder().id(2L).build();
        Message second = Message.builder().id(5L).build();
        var messages = List.of(first, second);
        var recipientsIds = List.of(currentUserId, 5);
        DeleteMessageResponse deleteMessageResponse = new DeleteMessageResponse(messagesIds, recipientsIds, senderId, chatId, forAll);
        when(messagesRepository.findAllById(messagesIds))
                .thenReturn(messages);
        when(messagesServiceTransactional.deleteMessageForAll(deleteMessageRequest, messages, currentUserId))
                .thenReturn(deleteMessageResponse);
        //when
        var result = messagesService.deleteMessages(deleteMessageRequest, currentUserId);
        //then
        assertNotNull(result);
        assertEquals(deleteMessageResponse, result);
        verify(messagesRepository, times(1)).findAllById(messagesIds);
        verify(messagesServiceTransactional, times(1)).deleteMessageForAll(deleteMessageRequest, messages, currentUserId);

        verify(messagesServiceTransactional, never())
                .deleteMessageForCurrentUser(any(), any(), anyInt());
    }
}