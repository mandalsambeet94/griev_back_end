package com.grievance.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttachmentUploadItemDTO {

    private Long attachmentId;
    private String uploadId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String s3Url;
    private String status;
}
