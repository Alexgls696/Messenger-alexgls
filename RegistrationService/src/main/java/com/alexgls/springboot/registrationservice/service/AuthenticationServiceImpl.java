package com.alexgls.springboot.registrationservice.service;

import com.alexgls.springboot.registrationservice.client.AuthServiceClient;
import com.alexgls.springboot.registrationservice.dto.AuthServiceJwtResponse;
import com.alexgls.springboot.registrationservice.dto.CheckCodeRequest;
import com.alexgls.springboot.registrationservice.dto.UserRegisterDto;
import com.alexgls.springboot.registrationservice.entity.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthServiceClient authServiceClient;

    private final VerificationService verificationService;

    @Transactional
    @Override
    public AuthServiceJwtResponse register(CheckCodeRequest checkCodeRequest) {
        UserData initializeUserData = verificationService.verifyLogin(checkCodeRequest);
        UserRegisterDto userRegisterDto = new UserRegisterDto(null, null, initializeUserData.getUsername(), "password", initializeUserData.getEmail());
        AuthServiceJwtResponse authServiceJwtResponse = authServiceClient.registerUser(userRegisterDto);
        verificationService.deleteById(checkCodeRequest.id());
        return authServiceJwtResponse;

    }

    @Transactional
    @Override
    public AuthServiceJwtResponse authenticateWithEmail(CheckCodeRequest checkCodeRequest) {
        UserData initializeUserData = verificationService.verifyLogin(checkCodeRequest);
        AuthServiceJwtResponse authServiceJwtResponse = authServiceClient.loginUserByEmail(initializeUserData.getEmail());
        verificationService.deleteById(checkCodeRequest.id());
        return authServiceJwtResponse;
    }

    @Override
    public AuthServiceJwtResponse authenticateWithPhoneNumber(CheckCodeRequest checkCodeRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
