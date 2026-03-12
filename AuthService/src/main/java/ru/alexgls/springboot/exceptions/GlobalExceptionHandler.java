package ru.alexgls.springboot.exceptions;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(ExistsUserRequestException.class)
    public ResponseEntity<ProblemDetail> handleCheckUsernameOrEmailMethodException(ExistsUserRequestException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                messageSource.getMessage("error.exists_user_request_exception", new Object[0], "error.exists_user_request_exception", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    @ExceptionHandler(UsernameExistsException.class)
    public ResponseEntity<ProblemDetail> handleUsernameAlreadyExistsException(UsernameExistsException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                messageSource.getMessage("error.username_already_exists", new Object[0], "error.username_already_exists", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                messageSource.getMessage("error.access_denied", new Object[0], "error.access_denied", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(problemDetail);
    }

    @ExceptionHandler(NoSuchUserException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchUserException(NoSuchUserException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_not_found");
    }

    @ExceptionHandler(NoSuchUserDetailsException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchUserDetailsException(NoSuchUserDetailsException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_details_not_found");
    }

    @ExceptionHandler(NoSuchUserAvatarException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchUserAvatarException(NoSuchUserAvatarException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_avatar_not_found");
    }

    @ExceptionHandler(NoSuchUserImageException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchUserImageException(NoSuchUserImageException exception, Locale locale) {
        return generateProblemDetailsByMessageForNotFound(exception, locale, "error.user_image_not_found");
    }

    @ExceptionHandler(ServiceUnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleServiceUnauthorizedException(ServiceUnauthorizedException exception, Locale locale) {
        log.warn("handleServiceUnauthorizedException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                messageSource.getMessage("error.unauthorized", new Object[0], "error.unauthorized", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(problemDetail);
    }

    private ResponseEntity<ProblemDetail> generateProblemDetailsByMessageForNotFound(Exception exception, Locale locale, String messageSourceValue) {
        log.warn("Handle {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messageSource.getMessage(messageSourceValue, new Object[0], messageSourceValue, locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }
}