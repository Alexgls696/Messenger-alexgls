package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.dto.messages.ChatMessage;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.ForwardMessageRequest;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.MessageDto;
import com.alexgls.springboot.messagestorageservicevt.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservicevt.service.KafkaSenderService;
import com.alexgls.springboot.messagestorageservicevt.service.MessagesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagesControllerTest {

    @InjectMocks
    MessagesController controller;

    @Mock
    MessagesService messagesService;

    @Mock
    KafkaSenderService kafkaSenderService;

    @Mock
    MessageMapper messageMapper;

    Authentication getAuthentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("userId", 1)
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void createMessage_ShouldBuildPayloadAndReturnSavedMessage(){
        //given
        ChatMessage chatMessage = ChatMessage
                .builder()
                .chatId(1L)
                .content("content")
                .tempId("tempId")
                .build();

        Authentication authentication = getAuthentication();

        MessageDto response = MessageDto.
                builder()
                .chatId(1L)
                .content("content")
                .tempId("tempId")
                .build();
        when(messagesService.save(any()))
                .thenReturn(response);
        //when

        var result = controller.createMessage(chatMessage,authentication);
        var body = result.getBody();

        //then
        verify(kafkaSenderService).sendMessage(response);
        ArgumentCaptor<CreateMessagePayload>payloadCaptor = ArgumentCaptor.forClass(CreateMessagePayload.class);
        verify(messagesService).save(payloadCaptor.capture());
        CreateMessagePayload payload = payloadCaptor.getValue();
        assertNotNull(payload);
        assertEquals(chatMessage.getChatId(), payload.chatId());
        assertEquals(chatMessage.getContent(), payload.content());
        assertEquals(chatMessage.getTempId(), payload.tempId());
        assertEquals(1, payload.senderId());

        assertEquals(response,body);
        assertEquals(200, result.getStatusCodeValue());
    }

    @Test
    void createForwardMessage_ShouldCallServiceSendKafkaAndReturnResponse() {
        // given
        ChatMessage chatMessage = ChatMessage.builder()
                .chatId(1L)
                .content("content")
                .tempId("tempId")
                .build();

        List<Long> forwardedIds = List.of(10L, 20L);

        ForwardMessageRequest request = new ForwardMessageRequest(
                chatMessage,
                forwardedIds
        );

        Authentication authentication = getAuthentication();

        CreateMessagePayload createMessagePayload = CreateMessagePayload
                .builder()
                .chatId(1L)
                .content("content")
                .tempId("tempId")
                .build();

        List<MessageDto> response = List.of(
                MessageDto.builder().chatId(1L).content("msg1").build(),
                MessageDto.builder().chatId(1L).content("msg2").build()
        );

        when(messageMapper.getCreateMessagePayload(chatMessage,authentication))
                .thenReturn(createMessagePayload);

        when(messagesService.saveMessageWithForwardedMessages(createMessagePayload, forwardedIds))
                .thenReturn(response);

        // when
        var result = controller.createForwardMessage(request, authentication);
        var body = result.getBody();

        // then

        ArgumentCaptor<CreateMessagePayload> captor =
                ArgumentCaptor.forClass(CreateMessagePayload.class);
        verify(messagesService)
                .saveMessageWithForwardedMessages(captor.capture(), eq(forwardedIds));
        CreateMessagePayload payload = captor.getValue();
        assertEquals(createMessagePayload, payload);

        verify(messagesService, times(1))
                .saveMessageWithForwardedMessages(any(), eq(forwardedIds));

        for (MessageDto msg : response) {
            verify(kafkaSenderService).sendMessage(msg);
        }

        verify(kafkaSenderService, times(response.size()))
                .sendMessage(any());

        assertNotNull(result);
        assertNotNull(body);
        assertEquals(200, result.getStatusCodeValue());
        assertEquals(response, body);
    }
}