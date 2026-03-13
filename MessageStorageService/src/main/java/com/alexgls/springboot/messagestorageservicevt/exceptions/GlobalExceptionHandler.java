package com.alexgls.springboot.messagestorageservicevt.exceptions;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException exception, Locale locale) {
        log.warn("handleAccessDeniedException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, messageSource
                .getMessage("errors.access_denied", new Object[0], "errors.access_denied", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
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

    @ExceptionHandler(NoSuchUserException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchUserException(NoSuchUserException exception, Locale locale) {
        log.warn("Handle NoSuchUserException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messageSource.getMessage("error.user_not_found", new Object[0], "error.user_not_found", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    @ExceptionHandler(NoSuchPinnedChatException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchPinnedChatException(NoSuchPinnedChatException exception, Locale locale) {
        log.warn("Handle NoSuchPinnedChatException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messageSource.getMessage("error.pinned_chat_not_found", new Object[0], "error.pinned_chat_not_found", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    @ExceptionHandler(NoSuchMessageException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchMessageException(NoSuchMessageException exception, Locale locale) {
        log.warn("Handle NoSuchMessageException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messageSource.getMessage("error.message_not_found", new Object[0], "error.message_not_found", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    @ExceptionHandler(NoSuchParticipantException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchParticipantException(NoSuchParticipantException exception, Locale locale) {
        log.warn("Handle NoSuchParticipantException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messageSource.getMessage("error.participant_not_found", new Object[0], "error.participant_not_found", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    @ExceptionHandler(NoSuchUsersChatException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchUsersChatException(NoSuchUsersChatException exception, Locale locale) {
        log.warn("Handle NoSuchUsersChatException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messageSource.getMessage("error.chat_not_found", new Object[0], "error.chat_not_found", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    @ExceptionHandler(DeleteMessageAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleDeleteMessageAccessDeniedException(DeleteMessageAccessDeniedException exception, Locale locale) {
        log.warn("Handle DeleteMessageAccessDeniedException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                messageSource.getMessage("errors.access_denied", new Object[0], "errors.access_denied", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(problemDetail);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> handleWebExchangeBindException(WebExchangeBindException exception, Locale locale) {
        log.warn("Handle WebExchangeBindException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                messageSource.getMessage("errors.validation.global", new Object[0], "errors.validation.global", locale));
        problemDetail.setProperty("error", exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .toList());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);

    }
}
