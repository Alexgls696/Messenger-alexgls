package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.dto.AiContentAnalysisRequest;
import com.alexgls.springboot.contentanalysisservice.dto.AnalysisResponse;

public interface AiContentAnalysisClient {
    AnalysisResponse analyzeTheFileById(AiContentAnalysisRequest aiContentAnalysisRequest, String token);
}
