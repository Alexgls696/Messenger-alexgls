package ru.alexgls.springboot.usersmessagingservice.client;


import ru.alexgls.springboot.usersmessagingservice.dto.JwtValidationResponse;

public interface AuthServiceClient {
    JwtValidationResponse validateToken(String token);
    String findUserInitialsById(int id,String token);
}
