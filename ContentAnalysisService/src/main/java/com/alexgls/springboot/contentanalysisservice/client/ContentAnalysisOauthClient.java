package com.alexgls.springboot.contentanalysisservice.client;

import com.alexgls.springboot.contentanalysisservice.dto.OauthResponse;

public interface ContentAnalysisOauthClient {
    OauthResponse getOauthTokenRequest();
}
