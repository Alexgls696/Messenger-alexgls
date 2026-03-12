package com.alexgls.springboot.indatabasecontentstorageservice.dto;

import java.sql.Timestamp;

public record CreateFileResponse(
        Integer id,
        String path,
        Timestamp createdAt
) {
}
