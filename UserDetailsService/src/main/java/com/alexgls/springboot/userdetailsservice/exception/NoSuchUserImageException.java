package com.alexgls.springboot.userdetailsservice.exception;

public class NoSuchUserImageException extends RuntimeException {
    public NoSuchUserImageException(String message) {
        super(message);
    }
}
