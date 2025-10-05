package com.alexgls.springboot.registrationservice.exception;

public class AccessToAuthServiceException extends RuntimeException {
    public AccessToAuthServiceException(String message) {
        super(message);
    }
}
