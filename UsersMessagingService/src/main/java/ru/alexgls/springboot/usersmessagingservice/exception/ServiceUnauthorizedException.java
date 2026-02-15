package ru.alexgls.springboot.usersmessagingservice.exception;

public class ServiceUnauthorizedException extends RuntimeException {
    public ServiceUnauthorizedException(String message) {
        super(message);
    }
}
