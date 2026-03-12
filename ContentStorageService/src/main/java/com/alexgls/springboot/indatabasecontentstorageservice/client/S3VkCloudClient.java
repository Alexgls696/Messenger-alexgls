package com.alexgls.springboot.indatabasecontentstorageservice.client;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.UploadUrlResponse;
import com.alexgls.springboot.indatabasecontentstorageservice.dto.UploadedFileDto;
import com.alexgls.springboot.indatabasecontentstorageservice.utils.FileCategoryDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3VkCloudClient {


    private final S3Presigner s3Presigner;

    @Value("${storage.bucket}")
    private String bucket;

    private final FileCategoryDetector categoryDetector;

    public UploadUrlResponse generateUploadUrl(String fileName, String contentType) {
        UUID fileId = UUID.randomUUID();

        String folder = categoryDetector.getFolderName(contentType);

        String key = String.format("%s/%s-%s", folder, fileId, fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new UploadUrlResponse(
                fileId,
                presignedRequest.url().toString(),
                key
        );
    }

    public String generateDownloadUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

}