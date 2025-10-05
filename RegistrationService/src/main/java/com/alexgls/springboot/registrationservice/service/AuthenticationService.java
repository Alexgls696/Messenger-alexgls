package com.alexgls.springboot.registrationservice.service;

import com.alexgls.springboot.registrationservice.dto.AuthServiceJwtResponse;
import com.alexgls.springboot.registrationservice.dto.CheckCodeRequest;

public interface AuthenticationService {
    AuthServiceJwtResponse register(CheckCodeRequest checkCodeRequest);

    AuthServiceJwtResponse authenticateWithEmail(CheckCodeRequest checkCodeRequest);

    AuthServiceJwtResponse authenticateWithPhoneNumber(CheckCodeRequest checkCodeRequest);

}
