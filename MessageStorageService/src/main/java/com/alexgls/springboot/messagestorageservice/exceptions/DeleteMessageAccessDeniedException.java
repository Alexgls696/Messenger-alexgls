package com.alexgls.springboot.messagestorageservice.exceptions;

public class DeleteMessageAccessDeniedException extends RuntimeException {
    public DeleteMessageAccessDeniedException(String message) {
        super(message);
    }
}
