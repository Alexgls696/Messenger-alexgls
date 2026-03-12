package com.alexgls.springboot.messagestorageservicevt.exceptions;

public class ServiceUnauthorizedException extends RuntimeException {
    public ServiceUnauthorizedException(String message) {
        super(message);
    }
}
