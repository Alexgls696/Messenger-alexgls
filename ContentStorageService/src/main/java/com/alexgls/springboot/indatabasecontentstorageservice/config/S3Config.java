package com.alexgls.springboot.indatabasecontentstorageservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${storage.access_key}")
    private String accessKey;

    @Value("${storage.secret_access_key}")
    private String secretKey;

    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.default_region}")
    private String defaultRegion;

    @Bean
    public S3Presigner s3Presigner() {

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(defaultRegion))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
    }

}