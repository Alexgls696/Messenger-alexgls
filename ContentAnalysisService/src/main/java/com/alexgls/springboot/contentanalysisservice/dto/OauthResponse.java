package com.alexgls.springboot.contentanalysisservice.dto;

public record OauthResponse(
        String access_token,
        long expires_as
) {
}
