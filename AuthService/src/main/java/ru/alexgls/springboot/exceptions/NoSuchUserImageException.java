package ru.alexgls.springboot.exceptions;

public class NoSuchUserImageException extends RuntimeException {
    public NoSuchUserImageException(String message) {
        super(message);
    }
}
