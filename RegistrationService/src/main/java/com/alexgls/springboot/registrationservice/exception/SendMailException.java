package com.alexgls.springboot.registrationservice.exception;

public class SendMailException extends RuntimeException {
    public SendMailException(String message) {
        super(message);
    }
}
