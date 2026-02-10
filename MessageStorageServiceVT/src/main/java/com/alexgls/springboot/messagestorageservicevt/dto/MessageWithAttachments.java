package com.alexgls.springboot.messagestorageservicevt.dto;

import com.alexgls.springboot.messagestorageservicevt.entity.Attachment;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MessageWithAttachments {
    private Message message;
    private List<Attachment> attachmentList;
}
