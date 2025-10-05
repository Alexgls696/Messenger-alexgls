package com.alexgls.springboot.registrationservice.client;

import com.alexgls.springboot.registrationservice.dto.AuthServiceExistsUserRequest;
import com.alexgls.springboot.registrationservice.dto.AuthServiceJwtResponse;
import com.alexgls.springboot.registrationservice.dto.AuthServiceUserExistsResponse;
import com.alexgls.springboot.registrationservice.dto.UserRegisterDto;


public interface AuthServiceClient {
    AuthServiceJwtResponse registerUser(UserRegisterDto userRegisterDto);

    AuthServiceJwtResponse loginUserByEmail(String email);

    AuthServiceJwtResponse loginUserByPhoneNumber(String phoneNumber);

    AuthServiceUserExistsResponse existsUserByUsernameOrEmail(AuthServiceExistsUserRequest authServiceExistsUserRequest);


}
