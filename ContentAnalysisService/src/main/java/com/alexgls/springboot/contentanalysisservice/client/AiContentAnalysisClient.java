package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.dto.AiContentAnalysisRequest;
import com.alexgls.springboot.contentanalysisservice.dto.AnalysisResponse;
import com.alexgls.springboot.contentanalysisservice.dto.LoadFileResponse;
import com.alexgls.springboot.contentanalysisservice.exception.LoadFileToAiException;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface AiContentAnalysisClient {
    AnalysisResponse analyzeTheFileById(AiContentAnalysisRequest aiContentAnalysisRequest);

    LoadFileResponse loadTheFile(Resource resource);
}
