package com.alexgls.springboot.userdetailsservice.exception;

public class ServiceUnauthorizedException extends RuntimeException {
    public ServiceUnauthorizedException(String message) {
        super(message);
    }
}
