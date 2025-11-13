package com.alexgls.springboot.contentanalysisservice.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Getter
@ToString
public class AiContentAnalysisRequest {

    private final String model = "GigaChat-2-Pro";
    private final String function_call = "auto";
    private final List<Message> messages = new ArrayList<>();


    @Getter
    @Setter
    private static class Message {
        private String role = "user";
        private String content = """
                Ты — интеллектуальный модуль анализа текстов. Твоя задача — выделять из документа ключевые темы, 
                смысловые категории и теги для последующего индексирования и быстрого поиска информации. 
                Работай строго в аналитическом стиле, не пересказывай текст. Не изменяй форму слов в тексте. 
                Собери как можно больше понятий из текста. Отвечай только в формате JSON, без комментариев, пояснений или текста вне структуры. 
                Проанализируй следующий документ и выдели из него смысловые категории, теги и краткое описание содержания. 
                Результат выдай строго в формате JSON со следующими полями:
                \\n\\n{\\n  \\"title\\": \\"краткое название документа\\",\\n  \\"summary\\": \\"одно-два предложения о сути документа\\"
                ,\\n  \\"topics\\": [\\"тема_1\\", \\"тема_2\\", \\"тема_3\\"],\\n  \\"keywords\\": [\\"ключевое_слово_1\\", 
                \\"ключевое_слово_2\\", \\"ключевое_слово_3\\"],\\n  \\"entities\\": [\\"имена\\", \\"организации\\", \\"технологии\\", \\"места (если есть)\\"]\\n}
                """;
        private List<String> attachments = new ArrayList<>();

        public Message(String fileId) {
            this.attachments.add(fileId);
        }
    }

    public AiContentAnalysisRequest(String fileId) {
        this.messages.add(new Message(fileId));
    }
}
