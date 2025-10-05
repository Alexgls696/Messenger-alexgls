package com.alexgls.springboot.registrationservice.controller;

import com.alexgls.springboot.registrationservice.dto.AuthServiceJwtResponse;
import com.alexgls.springboot.registrationservice.dto.CheckCodeRequest;
import com.alexgls.springboot.registrationservice.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/authentication")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid CheckCodeRequest checkCodeRequest, BindingResult bindingResult) throws BindException {
        log.info("Try to register user with code id: {} ", checkCodeRequest.id());
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        }
        AuthServiceJwtResponse jwtResponse = authenticationService.register(checkCodeRequest);
        if (Objects.isNull(jwtResponse)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Неверный код доступа или срок действия кода истёк"));
        } else {
            return ResponseEntity.ok(jwtResponse);
        }
    }

    @PostMapping("/login-by-email")
    public ResponseEntity<?> loginWithEmail(@RequestBody @Valid CheckCodeRequest checkCodeRequest, BindingResult bindingResult) throws BindException {
        log.info("Try to login user with email id: {} ", checkCodeRequest.id());
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        }
        AuthServiceJwtResponse jwtResponse = authenticationService.authenticateWithEmail(checkCodeRequest);
        if (Objects.isNull(jwtResponse)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Неверный код доступа или срок действия кода истёк"));
        } else {
            return ResponseEntity.ok(jwtResponse);
        }
    }
}
