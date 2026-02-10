package com.alexgls.springboot.messagestorageservicevt.dto;


public record CreateAttachmentPayload(
        Long fileId,
        String mimeType,
        String fileName,
        boolean hasAnalysis
) {
}