package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.dto.messages.*;
import com.alexgls.springboot.messagestorageservicevt.mapper.MessageMapper;
import com.alexgls.springboot.messagestorageservicevt.service.KafkaSenderService;
import com.alexgls.springboot.messagestorageservicevt.service.MessagesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


import java.util.List;
import java.util.Map;

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
    void createMessage_ShouldBuildPayloadAndReturnSavedMessage() {
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

        when(messageMapper.getCreateMessagePayload(chatMessage, authentication))
                .thenReturn(CreateMessagePayload
                        .builder()
                        .chatId(1L)
                        .content("content")
                        .tempId("tempId")
                        .senderId(1)
                        .build());
        //when

        var result = controller.createMessage(chatMessage, authentication);
        var body = result.getBody();

        //then
        verify(kafkaSenderService).sendMessage(response);
        ArgumentCaptor<CreateMessagePayload> payloadCaptor = ArgumentCaptor.forClass(CreateMessagePayload.class);
        verify(messagesService).save(payloadCaptor.capture());
        CreateMessagePayload payload = payloadCaptor.getValue();
        assertNotNull(payload);
        assertEquals(chatMessage.getChatId(), payload.chatId());
        assertEquals(chatMessage.getContent(), payload.content());
        assertEquals(chatMessage.getTempId(), payload.tempId());
        assertEquals(1, payload.senderId());

        assertEquals(response, body);
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

        when(messageMapper.getCreateMessagePayload(chatMessage, authentication))
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

    @Test
    void findMessagesByChatId_ShouldReturnMessageDtoList() {
        // given
        int chatId = 1;
        int page = 0;
        int size = 10;
        Authentication authentication = getAuthentication();

        List<MessageDto> messagesList = List.of(
                MessageDto.builder().chatId(chatId).content("content1").build(),
                MessageDto.builder().chatId(chatId).content("content2").build()
        );

        when(messagesService.getMessagesByChatId(eq(chatId), eq(page), eq(size), anyInt()))
                .thenReturn(messagesList);

        // when
        var response = controller.findMessagesByChatId(chatId, page, size, authentication);

        //then
        verify(messagesService, times(1))
                .getMessagesByChatId(eq(chatId), eq(page), eq(size), anyInt());

        assertEquals(messagesList, response);
        assertEquals(2, response.size());
    }

    @Test
    void findMessageById_ShouldReturnResponseEntityWithMessageDto_WhenSuccess() {
        //given
        long messageId = 73;
        long chatId = 12;
        Authentication authentication = getAuthentication();
        MessageDto messageDto = MessageDto
                .builder()
                .id(messageId)
                .chatId(chatId)
                .content("content")
                .build();

        when(messagesService.findById(eq(messageId), eq(chatId), anyInt()))
                .thenReturn(messageDto);
        //when

        var response = controller.findMessageById(messageId, chatId, authentication);

        //then
        verify(messagesService, times(1)).findById(eq(messageId), eq(chatId), anyInt());
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(messageDto, response.getBody());
    }

    @Test
    void findMessageById_ShouldReturnResponseEntityWithError_WhenMessageIdIsNull() {
        //given
        Long messageId = null;
        long chatId = 12;
        Authentication authentication = getAuthentication();

        //when

        var response = controller.findMessageById(messageId, chatId, authentication);

        //then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.containsKey("error"));
        assertNotNull(body.get("error"));
    }

    @Test
    void findMessageById_ShouldReturnResponseEntityWithError_WhenChatIdIsNull() {
        //given
        Long messageId = 53L;
        Long chatId = null;
        Authentication authentication = getAuthentication();

        //when

        var response = controller.findMessageById(messageId, chatId, authentication);

        //then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.containsKey("error"));
        assertNotNull(body.get("error"));
    }

    @Test
    void updateMessage_ShouldCallServiceSendKafkaAndReturnResponse() {
        //given
        long messageId = 73;
        EditMessageRequest editMessageRequest = new EditMessageRequest(2L, "new_content");
        Authentication authentication = getAuthentication();

        MessageDto resultDto = MessageDto
                .builder()
                .id(messageId)
                .chatId(editMessageRequest.chatId())
                .content("new_content")
                .build();

        when(messagesService.updateMessage(eq(messageId), anyInt(), eq(editMessageRequest)))
                .thenReturn(resultDto);

        //when

        var response = controller.updateMessage(messageId, editMessageRequest, authentication);
        //then

        verify(messagesService, times(1)).updateMessage(eq(messageId), anyInt(), eq(editMessageRequest));
        verify(kafkaSenderService, times(1)).sendUpdatedMessage(resultDto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(resultDto, response.getBody());
    }

    @Test
    void findMessagesByContent_ShouldReturnMessageDtoList() {
        //given
        SearchMessageInChatRequest request = new SearchMessageInChatRequest(1, "content");
        Authentication authentication = getAuthentication();

        var messages = List.of(MessageDto
                .builder()
                .chatId(request.chatId())
                .content("content")
                .build(), MessageDto
                .builder()
                .chatId(request.chatId())
                .content("conte")
                .build());
        when(messagesService.findMessagesByContent(eq(request), anyInt()))
                .thenReturn(messages);
        //when
        var result = controller.findMessagesByContent(request, authentication);

        //then
        verify(messagesService, times(1)).findMessagesByContent(eq(request), anyInt());
        assertEquals(messages, result);
    }

    @Test
    void readMessagesByIdsList_ShouldFilterOutOwnMessages() {
        // given
        int myUserId = 1;
        int otherUserId = 2;

        ReadMessagePayload myMessage = ReadMessagePayload.builder()
                .messageId(10L).senderId(myUserId).build();
        ReadMessagePayload otherMessage = ReadMessagePayload.builder()
                .messageId(11L).senderId(otherUserId).build();

        List<ReadMessagePayload> incomingList = List.of(myMessage, otherMessage);
        Authentication authentication = getAuthentication();

        // when
        controller.readMessagesByIdsList(incomingList, authentication);

        // then

        ArgumentCaptor<List<ReadMessagePayload>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(messagesService).readMessagesByList(listCaptor.capture(), eq(myUserId));

        List<ReadMessagePayload> capturedList = listCaptor.getValue();
        assertEquals(1, capturedList.size());
        assertEquals(11L, capturedList.get(0).messageId());

        verify(kafkaSenderService).sendReadMessagesToKafka(capturedList);
    }
    @Test
    void deleteMessage_ShouldReturnResponseAndCallKafkaService(){
        //given
        int senderId = 1;
        int chatId = 13;
        boolean forAll = true;
        var messagesIds = List.of(1L,2L,3L);
        DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(messagesIds, senderId, chatId, forAll);
        Authentication authentication = getAuthentication();

        DeleteMessageResponse deleteMessageResponse = new DeleteMessageResponse(messagesIds, List.of(2, 3), senderId,chatId,forAll);

        when(messagesService.deleteMessages(eq(deleteMessageRequest), anyInt()))
                .thenReturn(deleteMessageResponse);
        //when

        controller.deleteMessage(deleteMessageRequest, authentication);

        //then
        verify(messagesService, times(1)).deleteMessages(eq(deleteMessageRequest), anyInt());
        verify(kafkaSenderService, times(1)).sendDeleteEventMessagesToKafka(deleteMessageResponse);
    }
}