package com.alexgls.springboot.registrationservice.service;

import com.alexgls.springboot.registrationservice.dto.*;
import com.alexgls.springboot.registrationservice.entity.UserData;

public interface VerificationService {
    UserData verifyLogin(CheckCodeRequest checkCodeRequest);

    CreateCodeResponse createVerificationCodeForRegistration(InitializeLoginRequest initializeLoginRequest);

    CreateCodeResponse createVerificationCodeForAuthentication(InitializeLoginRequest initializeLoginRequest);

    void deleteById(String id);
}
