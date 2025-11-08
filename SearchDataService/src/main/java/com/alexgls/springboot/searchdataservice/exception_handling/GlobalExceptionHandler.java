package com.alexgls.springboot.searchdataservice.exception_handling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ProblemDetail> handleHttpClientErrorException(HttpClientErrorException exception, Locale locale) {
        log.info("handle HttpClientErrorException", exception);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(exception.getStatusCode().value()),
                messageSource.getMessage("errors.HttpClientErrorException", new Object[0], "errors.HttpClientErrorException", locale));
        problemDetail.setProperty("error", exception.getResponseBodyAsString());
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(problemDetail);
    }
}
