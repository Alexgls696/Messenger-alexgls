package com.alexgls.springboot.messagestorageservice.exceptions;

public class NoSuchParticipantException extends RuntimeException {
    public NoSuchParticipantException(String message) {
        super(message);
    }
}
