package com.alexgls.springboot.registrationservice.client;

import com.alexgls.springboot.registrationservice.dto.AuthServiceExistsUserRequest;
import com.alexgls.springboot.registrationservice.dto.AuthServiceJwtResponse;
import com.alexgls.springboot.registrationservice.dto.AuthServiceUserExistsResponse;
import com.alexgls.springboot.registrationservice.dto.UserRegisterDto;

import java.util.Map;

public interface AuthServiceClient {
    AuthServiceJwtResponse registerUser(UserRegisterDto userRegisterDto);

    AuthServiceUserExistsResponse existsUserByUsernameOrEmail(AuthServiceExistsUserRequest authServiceExistsUserRequest);
}
