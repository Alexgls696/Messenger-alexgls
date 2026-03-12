package com.alexgls.springboot.indatabasecontentstorageservice.exception_handling;

import com.alexgls.springboot.indatabasecontentstorageservice.exception.NoSuchImageException;
import com.alexgls.springboot.indatabasecontentstorageservice.exception.ServiceUnauthorizedException;
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
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(NoSuchImageException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(NoSuchImageException exception, Locale locale) {
        log.warn("Occurred exception: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, messageSource
                .getMessage("exception.no_such_image_exception", new Object[0], "exception.no_such_image_exception", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(problemDetail);

    }

    @ExceptionHandler(ServiceUnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleServiceUnauthorizedException(ServiceUnauthorizedException exception, Locale locale) {
        log.warn("handleServiceUnauthorizedException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                messageSource.getMessage("errors.unauthorized", new Object[0], "errors.unauthorized", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(problemDetail);
    }
}
