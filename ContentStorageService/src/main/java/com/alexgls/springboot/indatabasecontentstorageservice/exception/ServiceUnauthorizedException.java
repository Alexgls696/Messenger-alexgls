package com.alexgls.springboot.indatabasecontentstorageservice.exception;

public class ServiceUnauthorizedException extends RuntimeException {
    public ServiceUnauthorizedException(String message) {
        super(message);
    }
}
