package com.alexgls.springboot.messagestorageservicevt.exceptions;

public class ConnectionServiceException extends RuntimeException {
    public ConnectionServiceException(String message) {
        super(message);
    }
}
