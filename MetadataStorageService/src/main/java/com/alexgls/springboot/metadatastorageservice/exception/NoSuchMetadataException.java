package com.alexgls.springboot.metadatastorageservice.exception;

public class NoSuchMetadataException extends RuntimeException {
    public NoSuchMetadataException(String message) {
        super(message);
    }
}
