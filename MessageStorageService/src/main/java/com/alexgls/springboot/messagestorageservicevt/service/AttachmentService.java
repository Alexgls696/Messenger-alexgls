package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;

import java.util.List;

public interface AttachmentService {
    List<Attachment> findAllByMediaTypeAndChatId(String mediaType, int chatId, int currentUserId);
}
