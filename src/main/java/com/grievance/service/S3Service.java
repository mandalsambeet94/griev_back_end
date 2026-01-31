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
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.presigned-url-expiry:900}")
    private Integer presignedUrlExpiry; // seconds

    @Value("${file.upload.allowed-extensions:jpg,jpeg,png,pdf,doc,docx}")
    private String allowedExtensions;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private S3Presigner getS3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    // existing uploadFile (server-side) kept unchanged (for admin fallback)
    public String uploadFile(MultipartFile file, String key) throws IOException {
        validateFile(file);

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

    // generate presigned PUT URL for a specific key
    public String generatePresignedPutUrl(String key, String contentType, int expirySeconds) {
        validateFileName(key); // reuse validation if needed

        S3Presigner presigner = getS3Presigner();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .putObjectRequest(putObjectRequest)
                    .signatureDuration(Duration.ofSeconds(expirySeconds))
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (SdkServiceException e) {
            throw new FileStorageException("AWS Service error generating pre-signed URL: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new FileStorageException("AWS Client error generating pre-signed URL: " + e.getMessage(), e);
        }
    }

    // HEAD object to check existence & size
    public HeadObjectResponse headObject(String key) {
        S3Client s3Client = getS3Client();
        try {
            HeadObjectRequest headReq = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            return s3Client.headObject(headReq);
        } catch (S3Exception e) {
            return null;
        } catch (SdkServiceException e) {
            throw new FileStorageException("AWS Service error while checking object: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new FileStorageException("AWS Client error while checking object: " + e.getMessage(), e);
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

    public void validateContentType(String fileName, String contentType) {

        String extension = getFileExtension(fileName).toLowerCase();

        Map<String, String> allowed = Map.of(
                "jpg", "image/jpeg",
                "jpeg", "image/jpeg",
                "png", "image/png",
                "pdf", "application/pdf",
                "doc", "application/msword",
                "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        if (!allowed.containsKey(extension) ||
                !allowed.get(extension).equals(contentType)) {
            throw new FileStorageException("Invalid file type or content type");
        }
    }


    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    public String generateFileUrl(String key) {
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
