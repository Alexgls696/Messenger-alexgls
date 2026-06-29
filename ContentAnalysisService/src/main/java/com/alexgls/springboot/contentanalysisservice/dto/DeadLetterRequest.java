package com.alexgls.springboot.contentanalysisservice.dto;

public record DeadLetterRequest(
    int fileId,
    int chatId,
    String s3Key,
    String fileName,
    String errorType,
    String errorMessage,
    int retryCount,
    long firstFailureTime
) {}