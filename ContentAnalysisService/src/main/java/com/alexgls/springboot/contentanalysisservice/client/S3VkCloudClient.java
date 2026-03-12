package com.alexgls.springboot.contentanalysisservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;


import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class S3VkCloudClient {

    private final S3Client s3Client;


    @Value("${storage.bucket}")
    private String bucket;


    public InputStream downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest);
    }
}