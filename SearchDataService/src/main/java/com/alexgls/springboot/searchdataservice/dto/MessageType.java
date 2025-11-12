package com.alexgls.springboot.searchdataservice.dto;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    SYSTEM;

    private static final Map<String, MessageType> MIME_TO_MESSAGE_TYPE = new HashMap<>();

    static {

        // Изображения
        MIME_TO_MESSAGE_TYPE.put("image/jpeg", IMAGE);
        MIME_TO_MESSAGE_TYPE.put("image/png", IMAGE);
        MIME_TO_MESSAGE_TYPE.put("image/gif", IMAGE);
        MIME_TO_MESSAGE_TYPE.put("image/bmp", IMAGE);
        MIME_TO_MESSAGE_TYPE.put("image/webp", IMAGE);
        MIME_TO_MESSAGE_TYPE.put("image/svg+xml", IMAGE);

        // Видео
        MIME_TO_MESSAGE_TYPE.put("video/mp4", VIDEO);
        MIME_TO_MESSAGE_TYPE.put("video/mpeg", VIDEO);
        MIME_TO_MESSAGE_TYPE.put("video/ogg", VIDEO);
        MIME_TO_MESSAGE_TYPE.put("video/webm", VIDEO);
        MIME_TO_MESSAGE_TYPE.put("video/quicktime", VIDEO);
        MIME_TO_MESSAGE_TYPE.put("video/x-msvideo", VIDEO);

        // Аудио
        MIME_TO_MESSAGE_TYPE.put("audio/mpeg", AUDIO);
        MIME_TO_MESSAGE_TYPE.put("audio/ogg", AUDIO);
        MIME_TO_MESSAGE_TYPE.put("audio/wav", AUDIO);
        MIME_TO_MESSAGE_TYPE.put("audio/webm", AUDIO);
        MIME_TO_MESSAGE_TYPE.put("audio/aac", AUDIO);
        MIME_TO_MESSAGE_TYPE.put("audio/flac", AUDIO);

        // Файлы (документы, архивы и другие)
        MIME_TO_MESSAGE_TYPE.put("application/pdf", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/msword", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/vnd.ms-excel", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/vnd.ms-powerpoint", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/zip", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/x-rar-compressed", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/x-tar", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/gzip", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/octet-stream", FILE);

        MIME_TO_MESSAGE_TYPE.put("text/plain", FILE);
        MIME_TO_MESSAGE_TYPE.put("text/html", FILE);
        MIME_TO_MESSAGE_TYPE.put("text/css", FILE);
        MIME_TO_MESSAGE_TYPE.put("text/javascript", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/json", FILE);
        MIME_TO_MESSAGE_TYPE.put("application/xml", FILE);
    }

    public static MessageType fromMimeType(String mimeType) {
        return MIME_TO_MESSAGE_TYPE.getOrDefault(mimeType.toLowerCase(), FILE);
    }
}