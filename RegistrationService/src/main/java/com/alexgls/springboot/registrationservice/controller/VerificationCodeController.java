package com.alexgls.springboot.registrationservice.controller;

import com.alexgls.springboot.registrationservice.client.AuthServiceClient;
import com.alexgls.springboot.registrationservice.dto.*;
import com.alexgls.springboot.registrationservice.service.AuthenticationService;
import com.alexgls.springboot.registrationservice.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeController {

    private final VerificationService verificationService;


    @PostMapping("/create")
    public CreateCodeResponse createVerificationCode(@RequestBody InitializeLoginRequest initializeLoginRequest) {
        log.info("Creating verification code: ");
        return verificationService.createVerificationCodeForRegistration(initializeLoginRequest);
    }

    @PostMapping("/create-for-exists")
    public CreateCodeResponse createVerificationCodeForExists(@RequestBody InitializeLoginRequest initializeLoginRequest) {
        log.info("Creating verification code for exists: ");
        return verificationService.createVerificationCodeForAuthentication(initializeLoginRequest);
    }

}
