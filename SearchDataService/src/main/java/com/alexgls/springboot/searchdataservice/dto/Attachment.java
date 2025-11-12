package com.alexgls.springboot.searchdataservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
public class Attachment {


    private Long id;

    private Long messageId;

    private Integer chatId;

    private Long fileId;

    private String mimeType;

    private MessageType logicType;

    private String fileName;
}
