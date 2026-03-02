package ru.alexgls.springboot.exceptions;

public class NoSuchUserRoleException extends RuntimeException {
    public NoSuchUserRoleException(String message) {
        super(message);
    }
}
