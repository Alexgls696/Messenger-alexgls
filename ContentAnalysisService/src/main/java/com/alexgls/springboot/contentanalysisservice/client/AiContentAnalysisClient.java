package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.dto.AiContentAnalysisRequest;
import com.alexgls.springboot.contentanalysisservice.dto.AnalysisResponse;
import com.alexgls.springboot.contentanalysisservice.dto.LoadFileResponse;
import org.springframework.core.io.Resource;

public interface AiContentAnalysisClient {
    AnalysisResponse analyzeTheFileById(AiContentAnalysisRequest aiContentAnalysisRequest);

    LoadFileResponse loadTheFile(Resource resource);
}
