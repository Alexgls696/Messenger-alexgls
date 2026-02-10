package com.alexgls.springboot.messagestorageservicevt.exceptions;

public class NoSuchParticipantException extends RuntimeException {
    public NoSuchParticipantException(String message) {
        super(message);
    }
}
