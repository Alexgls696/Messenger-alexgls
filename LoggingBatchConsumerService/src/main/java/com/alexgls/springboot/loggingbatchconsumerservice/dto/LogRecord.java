package com.alexgls.springboot.loggingbatchconsumerservice.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LogRecord(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime timestamp,

        String level,

        String serviceName,

        String message,

        String traceId,

        String context
) {}