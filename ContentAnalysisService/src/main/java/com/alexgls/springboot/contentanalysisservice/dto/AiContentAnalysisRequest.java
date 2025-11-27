package com.alexgls.springboot.contentanalysisservice.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class AiContentAnalysisRequest {

    private final String model = "GigaChat-2";
    private final String function_call = "auto";
    private final List<Message> messages = new ArrayList<>();

    @Getter
    @Setter
    private static class Message {
        private String role = "user";
        private String content = """
                Ты — интеллектуальный модуль анализа текстов. Твоя задача — извлечь метаданные.
                        Инструкция:
                        1. Проанализируй текст и выдели смысловые категории.
                        2. Формат ответа: СТРОГО валидный JSON.
                        3. ВАЖНО: Верни JSON в одну строку (minified), без переносов строк, без отступов и без Markdown-разметку (без ```json).
                        4. Не добавляй никаких пояснений до или после JSON.
                        Требуемая структура JSON:
                        {
                          "title": "Заголовок",
                          "summary": "Суть в 1-2 предложениях",
                          "topics": ["тема1", "тема2"],
                          "keywords": ["ключ1", "ключ2"],
                          "entities": ["имена", "организации", "места"]
                        } Анализируй текст в файле, который я указал.""";
        private List<String> attachments = new ArrayList<>();

        public Message(String fileId) {
            this.attachments.add(fileId);
        }
    }

    public AiContentAnalysisRequest(String fileId) {
        this.messages.add(new Message(fileId));
    }
}
