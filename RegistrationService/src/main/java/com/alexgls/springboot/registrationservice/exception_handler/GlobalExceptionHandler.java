package com.alexgls.springboot.registrationservice.exception_handler;

import com.alexgls.springboot.registrationservice.exception.*;
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
        log.warn("Выброшено исключение: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.user_exists", new Object[0], "error.user_exists", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(UserNotFoundException exception, Locale locale) {
        log.warn("Выброшено исключение: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.user_not_found", new Object[0], "error.user_not_found", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ProblemDetail> handleAuthServiceException(AuthServiceException exception, Locale locale) {
        log.warn("Выброшено исключение: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.auth_service_error", new Object[0], "error.auth_service_error", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }

    @ExceptionHandler(SendMailException.class)
    public ResponseEntity<ProblemDetail> handleSendMailException(SendMailException exception, Locale locale) {
        log.warn("Выброшено исключение: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.send_mail_error", new Object[0], "error.send_mail_error", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }

    @ExceptionHandler(OperationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOperationNotFoundException(OperationNotFoundException exception, Locale locale) {
        log.warn("Выброшено исключение: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.operation_not_found", new Object[0], "error.operation_not_found", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }

    @ExceptionHandler(AccessToAuthServiceException.class)
    public ResponseEntity<ProblemDetail> handleAccessToAuthServiceException(AccessToAuthServiceException exception, Locale locale) {
        log.warn("Выброшено исключение: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, messageSource
                .getMessage("error.access_to_auth_service", new Object[0], "error.access_to_auth_service", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(problemDetail);
    }


}
