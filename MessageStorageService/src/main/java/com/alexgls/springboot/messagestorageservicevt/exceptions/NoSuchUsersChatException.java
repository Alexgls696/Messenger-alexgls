package com.alexgls.springboot.messagestorageservicevt.exceptions;

public class NoSuchUsersChatException extends RuntimeException {
    public NoSuchUsersChatException(String message) {
        super(message);
    }
}
