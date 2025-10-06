package com.alexgls.springboot.userdetailsservice.exception;

public class NoSuchUserAvatarException extends RuntimeException {
    public NoSuchUserAvatarException(String message) {
        super(message);
    }
}
