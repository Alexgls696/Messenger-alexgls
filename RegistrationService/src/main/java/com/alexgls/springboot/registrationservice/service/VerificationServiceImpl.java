package com.alexgls.springboot.registrationservice.service;

import com.alexgls.springboot.registrationservice.client.AuthServiceClient;
import com.alexgls.springboot.registrationservice.dto.*;
import com.alexgls.springboot.registrationservice.entity.InitializeUserData;
import com.alexgls.springboot.registrationservice.exception.UserExistsException;
import com.alexgls.springboot.registrationservice.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;

    private final AuthServiceClient authServiceClient;

    private final MailSenderService mailSenderService;

    @Transactional
    @Override
    public AuthServiceJwtResponse verifyLogin(CheckCodeRequest checkCodeRequest) {
        Optional<InitializeUserData> initializeUserDataOptional = verificationRepository.findById(checkCodeRequest.id());
        if (initializeUserDataOptional.isPresent()) {
            InitializeUserData initializeUserData = initializeUserDataOptional.get();
            if (initializeUserData.getCode().equals(checkCodeRequest.code())) {
                UserRegisterDto userRegisterDto = new UserRegisterDto(null, null, initializeUserData.getUsername(), "password", initializeUserData.getEmail());
                AuthServiceJwtResponse authServiceJwtResponse = authServiceClient.registerUser(userRegisterDto);
                verificationRepository.deleteById(checkCodeRequest.id());
                return authServiceJwtResponse;
            }
        }
        return null;
    }

    @Override
    public CreateCodeResponse createVerificationCodeForUser(InitializeLoginRequest initializeLoginRequest) {
        InitializeUserData initializeUserData = new InitializeUserData(UUID.randomUUID().toString(),
                generateVerificationCode(),
                initializeLoginRequest.username(),
                initializeLoginRequest.email(), initializeLoginRequest.phoneNumber());
        boolean exists = authServiceClient.existsUserByUsernameOrEmail(new AuthServiceExistsUserRequest(initializeLoginRequest.username(), initializeLoginRequest.email())).exists();
        if(!exists) {
            InitializeUserData saved = verificationRepository.save(initializeUserData);
            mailSenderService.sendMessage(initializeUserData.getEmail(),"Ваш код подтверждения ", initializeUserData.getCode());
            return new CreateCodeResponse(saved.getId());
        }
        throw new UserExistsException("Пользователь с введенными вами данными уже существует.");
    }

    public String generateVerificationCode() {
        List<Character> symbols = new ArrayList<>();
        for (char i = 'A'; i <= 'Z'; i++) {
            symbols.add(i);
        }
        for (char i = '0'; i <= '9'; i++) {
            symbols.add(i);
        }
        Collections.shuffle(symbols);
        return symbols.subList(0, 6)
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
