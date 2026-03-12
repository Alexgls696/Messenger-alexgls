package com.alexgls.springboot.indatabasecontentstorageservice.exception;

public class NoSuchImageException extends RuntimeException {
    public NoSuchImageException(String message) {
        super(message);
    }
}
