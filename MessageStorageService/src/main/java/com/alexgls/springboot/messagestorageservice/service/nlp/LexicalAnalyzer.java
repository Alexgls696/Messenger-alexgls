package com.alexgls.springboot.messagestorageservice.service.nlp;

import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Component
public class LexicalAnalyzer {

    private static final int MIN_TEXT_LENGTH_FOR_NLP = 3;

    private static class LanguageTools {
        final TokenizerME tokenizer;
        final POSTaggerME posTagger;
        final LemmatizerME lemmatizer;

        LanguageTools(TokenizerME tokenizer, POSTaggerME posTagger, LemmatizerME lemmatizer) {
            this.tokenizer = tokenizer;
            this.posTagger = posTagger;
            this.lemmatizer = lemmatizer;
        }
    }

    private final LanguageDetectorME languageDetector;
    private final Map<String, LanguageTools> toolsByLanguage;

    // Константы для кодов языков, которые возвращает модель OpenNLP
    private static final String LANG_CODE_ENGLISH = "eng";
    private static final String LANG_CODE_RUSSIAN = "rus";


    public LexicalAnalyzer() {
        try {
            // 1. Загружаем модель определения языка
            InputStream langModelStream = getResourceStream("/models/lang/langdetect.bin");
            LanguageDetectorModel langModel = new LanguageDetectorModel(langModelStream);
            this.languageDetector = new LanguageDetectorME(langModel);

            // 2. Инициализируем Map и загружаем в нее инструменты для каждого поддерживаемого языка
            this.toolsByLanguage = new HashMap<>();
            toolsByLanguage.put(LANG_CODE_ENGLISH, loadLanguageTools("en"));
            toolsByLanguage.put(LANG_CODE_RUSSIAN, loadLanguageTools("ru"));

        } catch (Exception e) {
            throw new RuntimeException("Error loading NLP models during initialization", e);
        }
    }

    private LanguageTools loadLanguageTools(String langPrefix) throws Exception {
        InputStream tokenModelStream = getResourceStream("/models/token/" + langPrefix + ".bin");
        InputStream posModelStream = getResourceStream("/models/pos/" + langPrefix + ".bin");
        InputStream lemmaModelStream = getResourceStream("/models/lemm/" + langPrefix + ".bin");

        TokenizerModel tokenModel = new TokenizerModel(tokenModelStream);
        POSModel posModel = new POSModel(posModelStream);
        LemmatizerModel lemmaModel = new LemmatizerModel(lemmaModelStream);

        TokenizerME tokenizer = new TokenizerME(tokenModel);
        POSTaggerME posTagger = new POSTaggerME(posModel);
        LemmatizerME lemmatizer = new LemmatizerME(lemmaModel);

        return new LanguageTools(tokenizer, posTagger, lemmatizer);
    }

    private InputStream getResourceStream(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Resource not found: " + path);
        }
        return stream;
    }


    public List<String> lemmatizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // УЛУЧШЕНИЕ 1: Пропускаем NLP для слишком короткого текста
        // Для таких строк лемматизация бессмысленна, а определение языка ненадежно.
        // Просто токенизируем по-простому и возвращаем.
        if (text.length() <= MIN_TEXT_LENGTH_FOR_NLP) {
            return simpleTokenize(text);
        }

        try {
            Language bestLanguage = languageDetector.predictLanguage(text);
            String langCode = bestLanguage.getLang();

            LanguageTools tools = toolsByLanguage.get(langCode);

            // ИСПРАВЛЕНИЕ 2: Безопасный Fallback
            if (tools == null) {
                System.out.println("Unsupported language detected: '" + langCode + "'. Falling back to simple tokenization.");
                // Вместо падения на английском токенизаторе, используем универсальный простой метод.
                return simpleTokenize(text);
            }

            String[] tokens = tools.tokenizer.tokenize(text);
            String[] posTags = tools.posTagger.tag(tokens);
            String[] lemmas = tools.lemmatizer.lemmatize(tokens, posTags);

            List<String> result = new ArrayList<>();
            for (int i = 0; i < tokens.length; i++) {
                String lemma = lemmas[i].equals("O") ? tokens[i].toLowerCase() : lemmas[i].toLowerCase();
                result.add(lemma);
            }
            return result;

        } catch (Exception e) {
            System.err.println("Error during NLP processing for: '" + text + "'. Falling back to simple tokenization.");
            e.printStackTrace();
            return simpleTokenize(text);
        }
    }

    private List<String> simpleTokenize(String text) {
        String[] tokens = text.toLowerCase().split("\\W+");
        return Arrays.stream(tokens)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}