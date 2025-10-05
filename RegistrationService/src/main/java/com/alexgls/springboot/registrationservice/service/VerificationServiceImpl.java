package com.alexgls.springboot.registrationservice.service;

import com.alexgls.springboot.registrationservice.client.AuthServiceClient;
import com.alexgls.springboot.registrationservice.dto.*;
import com.alexgls.springboot.registrationservice.entity.UserData;
import com.alexgls.springboot.registrationservice.exception.InvalidAccessCodeException;
import com.alexgls.springboot.registrationservice.exception.OperationNotFoundException;
import com.alexgls.springboot.registrationservice.exception.UserExistsException;
import com.alexgls.springboot.registrationservice.exception.UserNotFoundException;
import com.alexgls.springboot.registrationservice.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;

    private final AuthServiceClient authServiceClient;

    private final MailSenderService mailSenderService;

    @Override
    public UserData verifyLogin(CheckCodeRequest checkCodeRequest) {
        Optional<UserData> initializeUserDataOptional = verificationRepository.findById(checkCodeRequest.id());
        if (initializeUserDataOptional.isPresent()) {
            UserData initializeUserData = initializeUserDataOptional.get();
            if (initializeUserData.getCode().equals(checkCodeRequest.code())) {
                return initializeUserData;
            } else {
                throw new InvalidAccessCodeException("Неверный код доступа, повторите попытку.");
            }
        }
        throw new OperationNotFoundException("Операция не найдена.");
    }

    @Override
    public CreateCodeResponse createVerificationCodeForRegistration(InitializeLoginRequest initializeLoginRequest) {
        UserData initializeUserData = createUserData(initializeLoginRequest);
        boolean exists = authServiceClient.existsUserByUsernameOrEmail(new AuthServiceExistsUserRequest(initializeLoginRequest.username(), initializeLoginRequest.email())).exists();
        if (!exists) {
            UserData saved = verificationRepository.save(initializeUserData);
            mailSenderService.sendMessage(initializeUserData.getEmail(), "Ваш код подтверждения ", initializeUserData.getCode());
            return new CreateCodeResponse(saved.getId());
        }
        throw new UserExistsException("Пользователь с указанным адресом почты уже существует.");
    }

    @Override
    public CreateCodeResponse createVerificationCodeForAuthentication(InitializeLoginRequest initializeLoginRequest) {
        UserData initializeUserData = createUserData(initializeLoginRequest);
        boolean exists = authServiceClient.existsUserByUsernameOrEmail(new AuthServiceExistsUserRequest(initializeLoginRequest.username(), initializeLoginRequest.email())).exists();
        if (exists) {
            UserData saved = verificationRepository.save(initializeUserData);
            mailSenderService.sendMessage(initializeUserData.getEmail(), "Ваш код подтверждения ", initializeUserData.getCode());
            return new CreateCodeResponse(saved.getId());
        }
        throw new UserNotFoundException("Пользователь с указанным адресом электронной почты не найден");
    }

    @Override
    public void deleteById(String id) {
        verificationRepository.deleteById(id);
    }


    private UserData createUserData(InitializeLoginRequest initializeLoginRequest) {
        return new UserData(UUID.randomUUID().toString(),
                generateVerificationCode(),
                initializeLoginRequest.username(),
                initializeLoginRequest.email(), initializeLoginRequest.phoneNumber());
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
