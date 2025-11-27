package com.alexgls.springboot.contentanalysisservice.exception;

public class EmptyFileException extends RuntimeException {
    public EmptyFileException(String message) {
        super(message);
    }
}
