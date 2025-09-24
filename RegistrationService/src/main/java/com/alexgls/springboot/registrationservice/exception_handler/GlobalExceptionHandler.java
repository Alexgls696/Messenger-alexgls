package com.alexgls.springboot.registrationservice.exception_handler;

import com.alexgls.springboot.registrationservice.exception.AuthServiceException;
import com.alexgls.springboot.registrationservice.exception.UserExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(UserExistsException.class)
    public ResponseEntity<ProblemDetail> handleUserExistsException(UserExistsException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.user_exists", new Object[0], "error.user_exists", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ProblemDetail> handleAuthServiceException(AuthServiceException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.auth_service_error", new Object[0], "auth_service_error", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }
}
