package com.alexgls.springboot.userdetailsservice.exception;

public class NoSuchUserDetailsException extends RuntimeException {
    public NoSuchUserDetailsException(String message) {
        super(message);
    }
}
