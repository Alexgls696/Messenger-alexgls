package ru.alexgls.springboot.usersmessagingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Attachment {

    private Long id;
    private Long messageId;
    private Long fileId;
    private String mimeType;
    private String fileName;
}