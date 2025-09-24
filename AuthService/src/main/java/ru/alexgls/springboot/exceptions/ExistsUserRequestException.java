package ru.alexgls.springboot.exceptions;

public class ExistsUserRequestException extends RuntimeException {
    public ExistsUserRequestException(String message) {
        super(message);
    }
}
