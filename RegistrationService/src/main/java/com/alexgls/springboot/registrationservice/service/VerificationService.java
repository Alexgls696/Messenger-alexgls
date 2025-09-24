package com.alexgls.springboot.registrationservice.service;

import com.alexgls.springboot.registrationservice.dto.*;

public interface VerificationService {
    AuthServiceJwtResponse verifyLogin(CheckCodeRequest checkCodeRequest);
    CreateCodeResponse createVerificationCodeForUser(InitializeLoginRequest initializeLoginRequest);

}
