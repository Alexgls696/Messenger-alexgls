package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.dto.chats.ChatDto;
import com.alexgls.springboot.messagestorageservicevt.entity.Chat;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.repository.*;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatsServiceTest {

    @InjectMocks
    ChatsService chatsService;

    @Mock
    private ChatsRepository chatsRepository;

    @Mock
    private ParticipantsRepository participantsRepository;

    @Mock
    private MessagesRepository messagesRepository;

    @Mock
    private DeletedMessagesRepository deletedMessagesRepository;

    @Mock
    private AuthRestClient authRestClient;

    @Mock
    private PinnedChatsRepository pinnedChatsRepository;

    @Mock
    private KafkaSenderService kafkaSenderService;

    @Mock
    private EncryptUtils encryptUtils;

    @Mock
    private MessagesService messagesService;

    /*@Test
    void findChatById_WhenLastMessageExistsReturnsChatDto() {
        //given
        long chatId = 1L;
        int userId = 2;
        ChatDto expectedChatDto = ChatDto
                .builder()
                .chatId(chatId)
                .name("chat")
                .description("description")
                .type("PRIVATE")
                .build();
        //when

        when(chatsRepository.findById(chatId))
                .thenReturn(Optional.of(Chat.builder()
                        .id(chatId)
                        .name("chat")
                        .description("description")
                        .type("PRIVATE")
                        .build()));

        when(messagesRepository.findLastMessageByChatIdAndUserId(chatId, userId))
                .thenReturn(Optional.of(Message.builder()
                        .id(1L)
                        .content("encrypted")
                        .chatId(chatId)
                        .senderId(userId)
                        .build()));

        when(encryptUtils.decrypt(any()))
                .thenReturn("decrypted");

        //then
        var result = chatsService.findChatById(chatId, userId);
        ArgumentCaptor<Long> chatIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> userIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(messagesRepository).findLastMessageByChatIdAndUserId(chatIdCaptor.capture(), userIdCaptor.capture());
        int realUserId = userIdCaptor.getValue();
        long realChatId = chatIdCaptor.getValue();
        assertEquals(userId, realUserId);
        assertEquals(chatId, realChatId);

        assertEquals(chatId, result.getChatId());
        assertEquals("chat", result.getName());
        assertEquals("description", result.getDescription());
        assertEquals("PRIVATE", result.getType());
        assertNotNull(result.getLastMessage());
    }*/

    /*@Test
    void findChatById_WhenLastMessageIsNotExistsReturnsChatDtoWithoutLastMessage() {
        //given
        long chatId = 1L;
        int userId = 2;
        ChatDto expectedChatDto = ChatDto
                .builder()
                .chatId(chatId)
                .name("chat")
                .description("description")
                .type("PRIVATE")
                .build();
        //when

        when(chatsRepository.findById(chatId))
                .thenReturn(Optional.of(Chat.builder()
                        .id(chatId)
                        .name("chat")
                        .description("description")
                        .type("PRIVATE")
                        .build()));

        when(messagesRepository.findLastMessageByChatIdAndUserId(chatId, userId))
                .thenReturn(Optional.empty());

        //then
        var result = chatsService.findChatById(chatId, userId);
        ArgumentCaptor<Long> chatIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> userIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(messagesRepository).findLastMessageByChatIdAndUserId(chatIdCaptor.capture(), userIdCaptor.capture());
        int realUserId = userIdCaptor.getValue();
        long realChatId = chatIdCaptor.getValue();
        assertEquals(userId, realUserId);
        assertEquals(chatId, realChatId);

        assertEquals(chatId, result.getChatId());
        assertEquals("chat", result.getName());
        assertEquals("description", result.getDescription());
        assertEquals("PRIVATE", result.getType());
        assertNull(result.getLastMessage());
    }*/

    @Test
    void createPrivateChat_ReturnsChatDto() {
        //given
        int senderId = 2;
        int receiverId = 3;

        //when
        when(chatsRepository.save(any()))
                .thenReturn(Chat.builder()
                        .id(1L)
                        .name("chat")
                        .description("description")
                        .type("PRIVATE")
                        .build());
        when(participantsRepository.saveAll(anyList()))
                .thenReturn(List.of());
        //then

        var result = chatsService.createPrivateChat(senderId, receiverId);
        assertEquals(1L, result.getChatId());
        assertEquals("chat", result.getName());
        assertEquals("description", result.getDescription());
        assertEquals("PRIVATE", result.getType());
        assertNull(result.getLastMessage());
    }
}