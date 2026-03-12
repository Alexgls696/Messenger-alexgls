package com.alexgls.springboot.messagestorageservicevt.exceptions;

public class DeleteMessageAccessDeniedException extends RuntimeException {
    public DeleteMessageAccessDeniedException(String message) {
        super(message);
    }
}
