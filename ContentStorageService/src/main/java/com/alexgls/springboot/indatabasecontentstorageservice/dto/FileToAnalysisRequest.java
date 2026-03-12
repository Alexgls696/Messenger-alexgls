package com.alexgls.springboot.indatabasecontentstorageservice.dto;


public record FileToAnalysisRequest(
        String key,
        Integer fileId,
        Integer chatId
) {

}
