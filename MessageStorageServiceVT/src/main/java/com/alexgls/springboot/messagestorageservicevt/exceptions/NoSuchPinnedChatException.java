package com.alexgls.springboot.messagestorageservicevt.exceptions;

public class NoSuchPinnedChatException extends RuntimeException {
    public NoSuchPinnedChatException(String message) {
        super(message);
    }
}
