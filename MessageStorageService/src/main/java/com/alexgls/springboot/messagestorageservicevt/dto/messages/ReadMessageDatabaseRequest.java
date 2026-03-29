package com.alexgls.springboot.messagestorageservicevt.dto.messages;

import java.util.List;

public record ReadMessageDatabaseRequest
        (
                List<Long> messageIds,
                int chatId,
                int readerId,
                long lastReadMessageId,
                int countMessagesRead
        ) {
}