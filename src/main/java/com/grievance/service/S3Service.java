package com.grievance.service;

import com.grievance.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.presigned-url-expiry}")
    private Integer presignedUrlExpiry;

    @Value("${file.upload.allowed-extensions:jpg,jpeg,png,pdf,doc,docx}")
    private String allowedExtensions;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        // Validate file
        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        String key = folder + "/" + fileName;

        S3Client s3Client = getS3Client();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromBytes(file.getBytes()));

            return generateFileUrl(key);

        } catch (SdkServiceException e) {
            throw new FileStorageException("AWS Service error while uploading file: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new FileStorageException("AWS Client error while uploading file: " + e.getMessage(), e);
        }
    }

    public String generatePresignedUrl(String fileName, String contentType) {
        validateFileName(fileName);
        validateContentType(contentType);

        String key = "uploads/" + generateFileName(fileName);

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                    presign -> presign.signatureDuration(Duration.ofSeconds(presignedUrlExpiry))
                            .putObjectRequest(putObjectRequest));

            return presignedRequest.url().toString();

        } catch (SdkServiceException e) {
            throw new FileStorageException("AWS Service error generating pre-signed URL: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new FileStorageException("AWS Client error generating pre-signed URL: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String key) {
        S3Client s3Client = getS3Client();

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (SdkServiceException e) {
            throw new FileStorageException("AWS Service error deleting file: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new FileStorageException("AWS Client error deleting file: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 10MB");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new FileStorageException("File name is null");
        }

        validateFileName(fileName);
    }

    private void validateFileName(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        List<String> allowed = Arrays.asList(allowedExtensions.split(","));

        if (!allowed.contains(extension)) {
            throw new FileStorageException(
                    "File type not allowed. Allowed types: " + allowedExtensions);
        }
    }

    private void validateContentType(String contentType) {
        // You can add more specific content type validation if needed
        if (contentType == null || contentType.isEmpty()) {
            throw new FileStorageException("Content type is required");
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    private String generateFileName(String originalFileName) {
        String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return UUID.randomUUID().toString() + "_" + sanitizedFileName;
    }

    private String generateFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, key);
    }

    public String getFileUrl(String key) {
        S3Client s3Client = getS3Client();

        try {
            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            URL url = s3Client.utilities().getUrl(getUrlRequest);
            return url.toString();

        } catch (SdkServiceException e) {
            throw new FileStorageException("AWS Service error getting file URL: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new FileStorageException("AWS Client error getting file URL: " + e.getMessage(), e);
        }
    }
}