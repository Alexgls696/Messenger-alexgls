package com.alexgls.springboot.exception;

public class ServiceUnauthorizedException extends RuntimeException {
    public ServiceUnauthorizedException(String message) {
        super(message);
    }
}
