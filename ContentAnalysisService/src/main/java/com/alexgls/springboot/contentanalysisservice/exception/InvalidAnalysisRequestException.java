package com.alexgls.springboot.contentanalysisservice.exception;

public class InvalidAnalysisRequestException extends RuntimeException {
    public InvalidAnalysisRequestException(String message) {
        super(message);
    }
}
