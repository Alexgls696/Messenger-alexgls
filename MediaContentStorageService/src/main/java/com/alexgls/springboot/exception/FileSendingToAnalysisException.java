package com.alexgls.springboot.exception;

public class FileSendingToAnalysisException extends RuntimeException {
    public FileSendingToAnalysisException(String message) {
        super(message);
    }
}
