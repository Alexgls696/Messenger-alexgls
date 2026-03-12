package com.alexgls.springboot.indatabasecontentstorageservice.utils;

import org.springframework.stereotype.Component;

@Component
public class FileCategoryDetector {

    public String getFolderName(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return "others";
        }

        String type = contentType.toLowerCase();

        if (type.startsWith("image/")) {
            return "images";
        } else if (type.startsWith("video/")) {
            return "videos";
        } else if (type.startsWith("audio/")) {
            return "audio";
        } else if (type.startsWith("text/") ||
                type.contains("pdf") ||
                type.contains("msword") ||
                type.contains("officedocument")) {
            return "documents";
        } else if (type.contains("zip") ||
                type.contains("tar") ||
                type.contains("rar") ||
                type.contains("7z")) {
            return "archives";
        }

        return "others";
    }
}