package ru.alexgls.springboot.exceptions;

public class NoSuchUserAvatarException extends RuntimeException {
    public NoSuchUserAvatarException(String message) {
        super(message);
    }
}
