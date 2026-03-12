package ru.alexgls.springboot.usersmessagingservice.exception;

public class MessageStorageServiceException extends RuntimeException {
    public MessageStorageServiceException(String message) {
        super(message);
    }
}
