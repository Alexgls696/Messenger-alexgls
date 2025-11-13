package com.alexgls.springboot.contentanalysisservice.exception;

public class GetOauthTokenFailedException extends RuntimeException {
    public GetOauthTokenFailedException(String message) {
        super(message);
    }
}
