package com.fakru.interview.tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

@Service
@Slf4j
public class StorageService {

    private final S3Client s3Client;
    @Value("${application.bucket.name}")
    private String bucketName;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadFile(String fileName, MultipartFile file) {
        File fileObj = convertMultiPartFileToFile(file);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .build(),
                    RequestBody.fromFile(fileObj));
        } catch (S3Exception e) {
            log.error("Error uploading file to S3", e);
            throw e;
        } finally {
            fileObj.delete();
        }
    }

    public byte[] downloadFile(String fileName) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            System.err.println("The file does not exist in the bucket.");
        } catch (SdkClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String deleteFile(String fileName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        DeleteObjectResponse deleteObjectResponse = s3Client.deleteObject(deleteObjectRequest);

        return fileName + " removed, status: " + deleteObjectResponse.sdkHttpResponse().statusCode();
    }

    private File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            log.error("Error converting multipartFile to file", e);
        }
        return convertedFile;
    }
}