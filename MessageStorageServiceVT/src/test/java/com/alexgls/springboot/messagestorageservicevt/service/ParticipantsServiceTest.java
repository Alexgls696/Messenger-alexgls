package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ParticipantsServiceTest {

    @InjectMocks
    private ParticipantsService participantsService;

    @Mock
    private ParticipantsRepository participantsRepository;

    @Mock
    private MessagesService messagesService;

    @Mock
    private KafkaSenderService kafkaSenderService;

    @Mock
    private AuthRestClient authRestClient;



}