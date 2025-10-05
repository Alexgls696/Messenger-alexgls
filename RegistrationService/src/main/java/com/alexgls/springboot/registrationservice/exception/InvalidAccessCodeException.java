package com.alexgls.springboot.registrationservice.exception;

public class InvalidAccessCodeException extends RuntimeException {
    public InvalidAccessCodeException(String message) {
        super(message);
    }
}
