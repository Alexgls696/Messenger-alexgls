package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.entity.MessageToken;
import com.alexgls.springboot.messagestorageservicevt.repository.MessageTokenRepository;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import com.alexgls.springboot.messagestorageservicevt.service.nlp.LexicalAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LexicalAnalyserService {

    private final MessageTokenRepository messageTokenRepository;
    private final EncryptUtils encryptUtils;

    private final LexicalAnalyzer lexicalAnalyzer;

    @Transactional
    protected void saveMessageTokens(Message message) {
        if (message.getContent() == null || message.getContent().isEmpty() || message.isService()) {
            return;
        }
        String originalText = encryptUtils.decrypt(message.getContent());
        List<String> lemmas = lexicalAnalyzer.lemmatizeText(originalText);

        Set<String> uniqueHashes = new HashSet<>();
        for (String lemma : lemmas) {
            String hash = encryptUtils.calculateHmac(lemma);
            uniqueHashes.add(hash);
        }

        List<MessageToken> tokens = new ArrayList<>();
        for (String hash : uniqueHashes) {
            MessageToken token = new MessageToken(message.getId(), hash);
            tokens.add(token);
        }

        messageTokenRepository.saveAll(tokens);
    }
}
