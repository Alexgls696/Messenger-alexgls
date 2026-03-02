package ru.alexgls.springboot.exceptions;

public class ServiceUnauthorizedException extends RuntimeException {
    public ServiceUnauthorizedException(String message) {
        super(message);
    }
}
