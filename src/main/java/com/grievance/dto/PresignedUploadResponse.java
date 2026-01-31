package com.grievance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUploadResponse {
    private String uploadId;
    private String s3Key;
    private String presignedUrl;
    private String fileName;
    private String fileType;
}