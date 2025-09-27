package com.alexgls.springboot.registrationservice.service;


import com.alexgls.springboot.registrationservice.repository.VerificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock
    private VerificationRepository verificationRepository;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @Test
    public void handleCreateVerificationCodeForUser_CodeLengthMustBe6() {
        String code = verificationService.generateVerificationCode();
        assertEquals(6, code.length());
    }

}