package com.alexgls.springboot.registrationservice.controller;

import com.alexgls.springboot.registrationservice.dto.*;
import com.alexgls.springboot.registrationservice.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("api/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeController {
    private final VerificationService verificationService;

    @PostMapping("/create")
    public CreateCodeResponse createVerificationCode(@RequestBody InitializeLoginRequest initializeLoginRequest) {
        log.info("Creating verification code: ");
        return verificationService.createVerificationCodeForUser(initializeLoginRequest);
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkVerificationCode(@RequestBody @Valid CheckCodeRequest checkCodeRequest, BindingResult bindingResult) throws BindException {
        log.info("Checking verification code: {} ", checkCodeRequest);
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        }
        AuthServiceJwtResponse jwtResponse = verificationService.verifyLogin(checkCodeRequest);
        if (Objects.isNull(jwtResponse)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Неверный код доступа"));
        } else {
            return ResponseEntity.ok(jwtResponse);
        }
    }
}
