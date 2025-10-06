package com.alexgls.springboot.userdetailsservice.exception.handler;

import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserAvatarException;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserDetailsException;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserException;
import com.alexgls.springboot.userdetailsservice.exception.NoSuchUserImageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.Locale;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(NoSuchUserException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNoSuchUserException(NoSuchUserException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_not_found");
    }

    @ExceptionHandler(NoSuchUserDetailsException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNoSuchUserDetailsException(NoSuchUserDetailsException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_details_not_found");
    }

    @ExceptionHandler(NoSuchUserAvatarException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNoSuchUserAvatarException(NoSuchUserAvatarException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_avatar_not_found");
    }

    @ExceptionHandler(NoSuchUserImageException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNoSuchUserImageException(NoSuchUserImageException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_image_not_found");
    }

    private Mono<ResponseEntity<ProblemDetail>> generateProblemDetailsByMessageForNotFound(Exception exception, Locale locale, String messageSourceValue) {
        log.warn("Handle {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messageSource.getMessage(messageSourceValue, new Object[0], messageSourceValue, locale));
        problemDetail.setProperty("error", exception.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail));
    }
}
