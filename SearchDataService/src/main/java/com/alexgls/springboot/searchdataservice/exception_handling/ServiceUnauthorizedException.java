package com.alexgls.springboot.searchdataservice.exception_handling;

public class ServiceUnauthorizedException extends RuntimeException {
    public ServiceUnauthorizedException(String message) {
        super(message);
    }
}
