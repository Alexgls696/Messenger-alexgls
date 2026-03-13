package com.alexgls.springboot.messagestorageservicevt.repository.projection;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;

import java.util.List;

public interface AttachmentsByMessagesListProjection {

    Long getMessageId();

    Attachment getAttachment();
}