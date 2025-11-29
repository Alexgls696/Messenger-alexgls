package com.alexgls.springboot.metadatastorageservice.dto;



import java.util.List;


public record  FileMetadataResponse(

        int fileId,

        int chatId,

        String title,

        String summary,

        List<String> topics,

        List<String> keywords,

        List<String> entities
) {
}
