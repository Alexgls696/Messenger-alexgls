package ru.alexgls.springboot.usersmessagingservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MessageStorageServiceException.class)
    public void handleMessageStorageServiceException(final MessageStorageServiceException exception) {
        log.warn(exception.getMessage(), exception);
    }
}
