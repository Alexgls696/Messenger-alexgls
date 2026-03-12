package com.alexgls.springboot.messagestorageservicevt.dto.attachments;


public record CreateAttachmentPayload(
        Long fileId,
        String mimeType,
        String fileName,
        boolean hasAnalysis
) {
}