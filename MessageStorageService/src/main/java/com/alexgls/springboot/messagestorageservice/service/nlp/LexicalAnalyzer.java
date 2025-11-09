package com.alexgls.springboot.messagestorageservice.service.nlp;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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


    private static final String LANG_CODE_ENGLISH = "eng";
    private static final String LANG_CODE_RUSSIAN = "rus";


    public LexicalAnalyzer() {
        try {
            InputStream langModelStream = getResourceStream("/models/lang/langdetect.bin");
            LanguageDetectorModel langModel = new LanguageDetectorModel(langModelStream);
            this.languageDetector = new LanguageDetectorME(langModel);

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

        if (text.length() <= 3) {
            return simpleTokenize(text);
        }

        try {
            Language bestLanguage = languageDetector.predictLanguage(text);
            String langCode = bestLanguage.getLang();
            double confidence = bestLanguage.getConfidence();

            LanguageTools tools = toolsByLanguage.get(langCode);

            if (tools == null || confidence < 0.5) {
                log.warn("Language detection is unreliable for text: '{}'. Detected: '{}', confidence: {}. Attempting fallback.",
                        text, langCode, confidence);

                List<String> russianAttempt = tryLemmatizeWith(text, LANG_CODE_RUSSIAN);
                if (wasSuccessful(russianAttempt, text)) {
                    log.info("Successfully lemmatized as Russian (fallback).");
                    return russianAttempt;
                }

                List<String> englishAttempt = tryLemmatizeWith(text, LANG_CODE_ENGLISH);
                if (wasSuccessful(englishAttempt, text)) {
                    log.info("Successfully lemmatized as English (fallback).");
                    return englishAttempt;
                }

                log.warn("Fallback failed for text: '{}'. Using simple tokenization.", text);
                return simpleTokenize(text);
            }

            return lemmatizeWithTools(text, tools);

        } catch (Exception e) {
            return simpleTokenize(text);
        }
    }

    private List<String> tryLemmatizeWith(String text, String langCode) {
        LanguageTools tools = toolsByLanguage.get(langCode);
        if (tools == null) {
            return Collections.emptyList();
        }
        return lemmatizeWithTools(text, tools);
    }

    private List<String> lemmatizeWithTools(String text, LanguageTools tools) {
        String[] tokens = tools.tokenizer.tokenize(text.toLowerCase());
        String[] posTags = tools.posTagger.tag(tokens);
        String[] lemmas = tools.lemmatizer.lemmatize(tokens, posTags);

        List<String> result = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            String lemma = lemmas[i].equals("O") ? tokens[i] : lemmas[i];
            result.add(lemma);
        }
        return result;
    }


    private boolean wasSuccessful(List<String> lemmas, String originalText) {
        if (lemmas.isEmpty()) {
            return false;
        }
        String lemmatizedText = String.join("", lemmas);
        String cleanedOriginal = originalText.toLowerCase().replaceAll("\\W+", "");

        return !lemmatizedText.equals(cleanedOriginal);
    }

    private List<String> simpleTokenize(String text) {
        String[] tokens = text.toLowerCase().split("\\W+");
        return Arrays.stream(tokens)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}