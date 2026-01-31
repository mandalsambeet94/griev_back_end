package com.grievance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUploadRequest {
    private String grievanceId;
    private String uploadId; // optional - client provided idempotency key
    private String fileName;
    private String fileType; // PHOTO / DOCUMENT / OTHER
    private String contentType; // MIME type (image/jpeg)
}