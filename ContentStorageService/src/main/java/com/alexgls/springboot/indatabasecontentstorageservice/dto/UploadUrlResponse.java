package com.alexgls.springboot.indatabasecontentstorageservice.dto;

import java.util.UUID;

public record UploadUrlResponse(
        UUID fileId,
        String uploadUrl,
        String key
) {}