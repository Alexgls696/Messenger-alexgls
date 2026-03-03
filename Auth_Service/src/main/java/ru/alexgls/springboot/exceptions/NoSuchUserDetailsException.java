package ru.alexgls.springboot.exceptions;

public class NoSuchUserDetailsException extends RuntimeException {
    public NoSuchUserDetailsException(String message) {
        super(message);
    }
}
