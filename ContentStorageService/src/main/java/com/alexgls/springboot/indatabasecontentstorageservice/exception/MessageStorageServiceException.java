package com.alexgls.springboot.indatabasecontentstorageservice.exception;

public class MessageStorageServiceException extends RuntimeException {
    public MessageStorageServiceException(String message) {
        super(message);
    }
}
