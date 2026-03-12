package ru.alexgls.springboot.exceptions;

public class NoSuchAuthException extends RuntimeException {
    public NoSuchAuthException(String message) {
        super(message);
    }
}
