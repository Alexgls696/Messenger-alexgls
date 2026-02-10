package com.alexgls.springboot.messagestorageservicevt.exceptions;

public class NoSuchRecipientException extends RuntimeException {
    public NoSuchRecipientException(String message) {
        super(message);
    }
}
